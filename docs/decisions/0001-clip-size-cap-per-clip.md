# 0001. Raise a clip's size cap per clip, not for every clip

- Status: Accepted (2026-11-08)
- Source: PR #83

## Context
`scripts/record.py` raises the CRF until a clip fits in 5 MB, and has done since the first commit. Clips are kept in git, so small files keep the repo light. The sand mandala demo, extended in #83 to show every way in and out, moves about 20,000 grains of sand for 37.5 s. That is close to the worst case for H.264: the clip was still 11.6 MB at CRF 34, the highest CRF `record.py` tries, so it refused the recording. The frost card clip from the same PR fit in 4.7 MB.

## Decision
`record.py` takes `--max-mb` (default 5), and the sand mandala clip is recorded with `--max-mb 15` (15.2 MB). The 5 MB default stays for every other clip, so a clip only gets bigger when someone asks for it on the command line, and the sand clip keeps every path its demo plays.

## Alternatives
- Raise the 5 MB cap for every clip. Rejected: clips that fit today would grow too, with no one choosing it.
- Cut a path from the sand demo to make it shorter. Rejected: every clip must show all of a component's features, and even without one path it would likely still be over 5 MB.
- Encode the sand clip at a lower frame rate or size. Rejected: the sand's motion would look rougher.

## Consequences
A clip over 5 MB has to be recorded with `--max-mb`, and the README shows how. `record.py`'s error now suggests it next to shortening the script. Video platforms accept far larger files, so the cap is about keeping clips small, not about upload limits. Worth revisiting if more demos need a raised cap: at that point a per-demo size setting in the catalog would beat remembering a flag.
