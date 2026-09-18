import argparse
import hashlib
import json
import shutil
import sys
import urllib.request
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CACHE = ROOT / "run" / "downloads"
API = "https://api.modrinth.com/v2"
USER_AGENT = "iA7MD/ae2-rs2-pattern-converter/fetch_libs (github.com/iA7MD-7)"

LIBS = "libs"
CLIENT = "run/mods"
CLIENT_DISABLED = "run/mods_disabled"
SERVER = "run/server/mods"
GAMETEST = "run/gametest/mods"
ALL = (LIBS, CLIENT, SERVER, GAMETEST)
RUNTIME = (CLIENT, SERVER, GAMETEST)

MODS = [
    ("refined-storage", "refinedstorage-neoforge-3.2.1.jar", ALL),
    ("ae2", "appliedenergistics2-26.1.11-beta.jar", ALL),
    ("guideme", "guideme-26.1.12-beta.jar", ALL),
    ("jei", "jei-26.1.2-neoforge-29.37.0.99.jar", ALL),
    ("advancedae", "AdvancedAE-26.1.7.jar", ALL),
    ("extended-ae", "ExtendedAE-26.1-1.0.3-neoforge.jar", (LIBS, CLIENT_DISABLED, SERVER, GAMETEST)),
    ("glodium", "Glodium-26.1-1.2-neoforge.jar", (LIBS, CLIENT_DISABLED, SERVER, GAMETEST)),
    ("appflux", "AppliedFlux-26.1-1.0.1-neoforge.jar", (LIBS, CLIENT_DISABLED, SERVER, GAMETEST)),
    ("refined-types", "refined-types-26.1.2-1.0.0.jar", ALL),
    ("geckolib", "geckolib-neoforge-26.1.2-5.5.2.jar", RUNTIME),
    ("applied-energistics-2-wireless-terminals", "ae2wtlib-26.1.1-beta.jar", RUNTIME),
    ("refined-storage-jei-integration", "refinedstorage-jei-integration-neoforge-2.0.1.jar", (CLIENT, GAMETEST)),
]

NESTED = [
    ("AdvancedAE-26.1.7.jar", "META-INF/jarjar/ae2addonlib-26.1.3-alpha.jar"),
    ("AdvancedAE-26.1.7.jar", "META-INF/jarjar/ae2wtlib_api-26.1.1-beta.jar"),
]


def log(message):
    print(message, flush=True)


def api(path):
    request = urllib.request.Request(API + path, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=60) as response:
        return json.load(response)


def sha512(path):
    digest = hashlib.sha512()
    with open(path, "rb") as handle:
        for chunk in iter(lambda: handle.read(1 << 20), b""):
            digest.update(chunk)
    return digest.hexdigest()


def locate(slug, filename):
    for version in api(f"/project/{slug}/version"):
        for file in version["files"]:
            if file["filename"] == filename:
                return file["url"], file["hashes"]["sha512"]
    raise SystemExit(f"{filename} is no longer listed under modrinth.com/mod/{slug}; update the manifest")


def download(slug, filename):
    target = CACHE / filename
    url, expected = locate(slug, filename)
    if target.exists() and sha512(target) == expected:
        return target
    log(f"  downloading {filename}")
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=300) as response, open(target, "wb") as handle:
        shutil.copyfileobj(response, handle)
    if sha512(target) != expected:
        target.unlink()
        raise SystemExit(f"{filename}: checksum mismatch after download")
    return target


def stage(source, targets):
    for target in targets:
        directory = ROOT / target
        directory.mkdir(parents=True, exist_ok=True)
        destination = directory / source.name
        if not destination.exists() or destination.stat().st_size != source.stat().st_size:
            shutil.copy2(source, destination)


def extract_nested(parent, member):
    destination = ROOT / LIBS / Path(member).name
    if destination.exists():
        return
    with zipfile.ZipFile(parent) as archive:
        with archive.open(member) as inner, open(destination, "wb") as handle:
            shutil.copyfileobj(inner, handle)
    log(f"  extracted {destination.name} from {parent.name}")


def main():
    parser = argparse.ArgumentParser(description="Download the exact dependency jars this branch compiles and runs against.")
    parser.add_argument("--libs-only", action="store_true", help="only populate libs/ (what a build needs); skip the run directories")
    args = parser.parse_args()
    CACHE.mkdir(parents=True, exist_ok=True)
    for slug, filename, targets in MODS:
        if args.libs_only:
            targets = tuple(t for t in targets if t == LIBS)
            if not targets:
                continue
        log(f"{filename}")
        stage(download(slug, filename), targets)
    for parent, member in NESTED:
        extract_nested(CACHE / parent, member)
    log("done")


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        sys.exit(130)
