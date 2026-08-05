# EutherTime generated alarm profiles

Generated locally on 2026-08-05 through the running EutherStudio worker and ACE-Step 1.5 turbo model. All three jobs were instrumental, 24 seconds, and completed through the shared GPU scheduler. Android retains the selected system alarm as a playback fallback.

## Masters

| Profile | Job ID | Prompt summary | WAV SHA-256 |
| --- | --- | --- | --- |
| Neon Dawn | `euthertime-neon-dawn-20260805-v1` | Warm glassy synth bells, analog pad, 68 BPM, hopeful repeating melody | `4b0dffd8e4f405716bd7c4420183d501a39dfaabe6faf985c6e71531ba9f9d2d` |
| Pulse Grid | `euthertime-pulse-grid-20260805-v1` | Digital marimba motif, rounded pulse, bright arpeggio, 92 BPM | `eb3414185f8e816722d5b8413fe8d712ad97fd3605877253825a03ceb84f42ce` |
| Red Shift | `euthertime-red-shift-20260805-v1` | Urgent syncopated synth, FM bells, electronic drums, 124 BPM | `0d8352424471f61dd56690d9abac7a38e467692eaa8e1cc6a1028daa909b3430` |

The source WAV masters remain under `/home/nichlas/ai/eutherstudio/worker/work/output/` and are not bundled into the APK.

## Android copies

ACE-Step added a silent tail to each master. The bundled copies were trimmed at the measured end of audible content, normalized, given only a 120 ms entrance and 180 ms exit seam, and encoded with:

```sh
ffmpeg -i INPUT.wav -af "atrim=end=MEASURED_END,asetpts=PTS-STARTPTS,loudnorm=I=-16:LRA=7:TP=-1.5,afade=t=in:st=0:d=0.12,afade=t=out:st=FADE_START:d=0.18" -ar 48000 -c:a libvorbis -q:a 5 OUTPUT.ogg
```

| Resource | Size | Ogg SHA-256 |
| --- | ---: | --- |
| `euthertime_neon_dawn.ogg` | 446137 bytes | `257991cb57264c225031f237299f21500e8db3b547ad547ef77ff5ffd6d0798d` |
| `euthertime_pulse_grid.ogg` | 396720 bytes | `fa43d780f70394fc7d3790e6677ca88ef0f62bb53e8e8fe5e92b584a7dbb6ba4` |
| `euthertime_red_shift.ogg` | 338906 bytes | `8f04583eab30a45fc9910f6dd43c3b052a57631f887a8eeb7904b7b6e1cd8cc8` |

Total bundled audio is approximately 1.2 MB instead of 13.8 MB for the three PCM WAV masters. A silence scan found no remaining segment longer than 0.5 seconds.
