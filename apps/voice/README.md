# Spirit box voices

Makes the clips the spirit box speaks: every trait word (`word.restless.*`) and answer (`spirit_box.restless.*`) of
`en_us` and `ru_ru`, read by [Piper](https://github.com/OHF-Voice/piper1-gpl) and turned into a whisper (each
frame's linear-prediction filter driven by noise instead of the voice), then broken up by a dead radio (lower and
slower, band-passed, crushed, cut out, static, echo or a backwards swell), plus a few bursts of
static. Writes `apps/mod/core/src/main/resources/assets/restless/sounds/spirit_box` and the `spirit_box.*` entries
of `sounds.json`; the rest of `sounds.json` is kept.

```sh
mise run voice:generate   # sets up .venv (uv, Python 3.12) and regenerates everything
```

Run it again after a trait word or an answer is added or retranslated. Voices (`en_US-joe-medium`,
`ru_RU-dmitri-medium`, CC0 data) are downloaded into `voices/` on the first run. Piper's output varies a little
between runs, so every clip changes on regeneration.
