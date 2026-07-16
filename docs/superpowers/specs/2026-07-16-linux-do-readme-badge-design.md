# LINUX DO README Badge Design

## Goal

Add a reliable LINUX DO badge to TomlJump's public README headers and point all project discussion links to the maintainer's TomlJump topic at `https://linux.do/t/topic/2589906`.

## Design

- Use the direct `img.shields.io` badge with the embedded LINUX DO logo supplied by the maintainer.
- Place the badge inside the existing centered technology badge row so the open-source header remains restrained and consistent.
- Link the badge directly to the TomlJump discussion topic rather than the LINUX DO home page.
- Replace the existing generic Linux DO community link in both `README.md` and `README.zh-CN.md` with the same topic URL.
- Keep the English and Chinese README structures aligned.

The `shorturl.at` image option was rejected because it adds an opaque redirect dependency and currently responds with a Cloudflare HTTP 403 challenge, making GitHub rendering less reliable.

## Verification

- Confirm both README files contain the same badge image source and topic URL.
- Confirm the old generic `https://linux.do/` links are gone.
- Run `git diff --check` and inspect the rendered-header HTML structure for matching tags.
