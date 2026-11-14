"""Cuts the owner's horror pack into sounds/haunt and synthesises the music box and mirror ring notes.

Run with the apps/voice venv, from outside the unzipped pack: python -I haunt_pack.py "<unzipped>/horror sounds" <out dir>.
The raw pack is never committed. Args: <pack dir> <out dir>.
"""
import sys, os, numpy as np, soundfile as sf
from scipy.signal import resample_poly
from math import gcd
PACK, OUT = sys.argv[1], sys.argv[2]
SR = 44100
PEAK = 10 ** (-3 / 20)
J = os.path.join(PACK, "jump scares")
G = os.path.join(PACK, "generic thriller")
report = []

def load(path):
    d, sr = sf.read(path, always_2d=True)
    if sr != SR:
        g = gcd(sr, SR)
        d = resample_poly(d, SR // g, sr // g, axis=0)
    return d

def find(folder, start):
    hits = [f for f in os.listdir(folder) if f.startswith(start)]
    assert len(hits) == 1, (start, hits)
    return os.path.join(folder, hits[0])

def fades(y, fin=0.002, fout=0.02):
    a, b = int(fin * SR), int(fout * SR)
    if a: y[:a] *= np.linspace(0, 1, a)[:, None]
    if b: y[-b:] *= np.linspace(1, 0, b)[:, None]
    return y

def trim(y, db):
    env = np.abs(y).max(1)
    thr = env.max() * 10 ** (db / 20)
    idx = np.where(env > thr)[0]
    return y[max(idx[0] - int(0.003 * SR), 0): idx[-1] + 1]

def write(name, y, mono, src, note=""):
    if mono and y.shape[1] > 1:
        side = np.sqrt(((y[:, 0] - y[:, 1]) ** 2).mean()); mid = np.sqrt(((y[:, 0] + y[:, 1]) ** 2).mean())
        if mid and side / mid > 0.5:  # wide or out of phase: averaging would cancel, keep the louder channel
            y = y[:, [int(np.argmax((y ** 2).mean(0)))]]
            note += f" side/mid {side/mid:.2f}, louder channel kept"
        else:
            y = y.mean(1, keepdims=True)
            note += f" side/mid {side/mid:.2f}, channels averaged"
    y = y / np.abs(y).max() * PEAK
    sf.write(os.path.join(OUT, name + ".ogg"), y, SR, format="OGG", subtype="VORBIS")
    report.append(f"{name}\t{len(y)/SR:.2f}s\t{y.shape[1]}ch\t{src}\t{note}")

def oneshot(name, path, a=None, b=None, db=-45, mono=True, fout=0.02):
    y = load(path)
    if a is not None: y = y[int(a * SR): int(b * SR) if b else None]
    y = fades(trim(y, db).copy(), fout=fout)
    write(name, y, mono, os.path.basename(path) + (f" [{a}-{b}s]" if a is not None else ""))

def loop(name, path, a, b, x=2.0):
    y = load(path)
    L, X = int((b - a) * SR), int(x * SR)
    seg = y[int(a * SR): int(a * SR) + L + X]
    out = seg[:L].copy(); tail = seg[L:L + X]
    t = np.linspace(0, np.pi / 2, X)[:, None]
    out[:X] = out[:X] * np.sin(t) + tail * np.cos(t)
    write(name, out, False, os.path.basename(path) + f" [{a}-{b}s], {x}s cross-fade loop")

# S1: haunt.jolt
oneshot("jolt_1", find(J, "Impact Slam Jumpscare Sound Effect 1"), db=-40)
oneshot("jolt_2", find(J, "Horror Stab Jumpscare Sound Effect.ogg"), db=-40)
oneshot("jolt_3", find(J, "Impact Slam Jumpscare Sound Effect 3"), db=-40)
# S4 part 1: one family per kind of scare
for name, files in {
    "sting_roar": ["Jumpscare Roar Sound Effect.ogg", "Jumpscare Roar Sound Effect 2", "Jumpscare Roar Sound Effect 4"],
    "sting_scream": ["Echoing Scream Jumpscare Sound Effect.ogg", "Echoing Scream Jumpscare Sound Effect 1", "Echoing Scream Jumpscare Sound Effect 2"],
    "sting_violin": ["Screeching Violin Jumpscare Sound Effect 1 - sharp", "Screeching Violin Jumpscare Sound Effect 2", "Screeching Violin Jumpscare Sound Effect 3"],
    "sting_bass": ["Bass Drop Shock Jumpscare Sound Effect 1", "Bass Drop Shock Jumpscare Sound Effect 2", "Bass Drop Shock Jumpscare Sound Effect 3"],
    "sting_blast": ["Sound Blast Jumpscare Sound Effect.ogg", "Sound Blast Jumpscare Sound Effect 1", "Sound Blast Jumpscare Sound Effect 3"],
}.items():
    for i, f in enumerate(files, 1):
        oneshot(f"{name}_{i}", find(J, f), db=-40)
# S4 part 2: beds (stereo, played on the player)
loop("blight_bed", find(G, "Creepy Horror Atmosphere"), 8.0, 38.0)
loop("sanity_bed", find(G, "suspense-dark-ambient"), 8.0, 32.0)
loop("ritual_fury", find(G, "Tense & Pressured"), 5.5, 35.5)
# S4 part 3: stings for moments
oneshot("evolve", find(G, "rising-evil"), 0, 5.0, fout=0.3)
oneshot("offering_refused", find(G, "metallic-surge"), 0, 3.5, fout=0.2)
oneshot("ritual_leaving", find(G, "build-up-of-transition"), 0, 3.75, fout=0.03)
oneshot("requiem", find(G, "build-up-of-transition"), 5.2, 8.75, fout=0.03)
oneshot("laid_to_rest", find(G, "Horror Swell"), 0, 5.0, fout=0.3)
# S4 part 4: one-shots
oneshot("heartbeat_1", find(G, "sfx-slow-powerful-heartbeat"), 1.72, 3.35)
oneshot("heartbeat_2", find(G, "sfx-slow-powerful-heartbeat"), 6.55, 8.15)
for i, (a, b) in enumerate([(0.3, 2.4), (3.6, 5.8), (7.9, 10.0)], 1):
    oneshot(f"chain_rattle_{i}", find(G, "chain-rattling"), a, b, fout=0.1)
for i, (a, b) in enumerate([(2.9, 6.3), (7.95, 10.7), (11.95, 14.3)], 1):
    oneshot(f"creature_rattle_{i}", find(G, "Alien Monster Noises"), a, b, fout=0.15)
oneshot("laugh_2", find(G, "Horror Kid Laugh"), 0, 4.0, fout=0.2)
oneshot("screech_2", find(G, "Demonic Woman Scream"), fout=0.1)

# Synthesised (S1)
rng = np.random.default_rng(7)
def partials(f0, spec, dur, attack):
    t = np.arange(int(dur * SR)) / SR
    y = np.zeros_like(t)
    for ratio, amp, tau, beat in spec:
        f = f0 * ratio
        if f > SR / 2.2: continue
        tone = np.sin(2 * np.pi * f * t + rng.uniform(0, 2 * np.pi))
        if beat:  # a detuned twin for shimmer
            tone = 0.5 * (tone + np.sin(2 * np.pi * (f + beat) * t))
        y += amp * tone * np.exp(-t / tau)
    a = int(attack * SR); y[:a] *= np.linspace(0, 1, a)
    return y

# Music box: F#4 (note block note 12 at 1.0x) = 369.99 Hz; comb tine partials are inharmonic beam modes.
box = partials(369.99, [(1, 1.0, 0.42, 0), (2.0, 0.18, 0.25, 0), (5.93, 0.22, 0.07, 0), (3.01, 0.06, 0.15, 0),
                        (16.6, 0.05, 0.015, 0)], 1.3, 0.001)
click = rng.standard_normal(int(0.004 * SR)) * np.exp(-np.arange(int(0.004 * SR)) / (0.0008 * SR))
click = np.diff(click, prepend=0) * 0.25
box[:len(click)] += click
write("music_box_note", fades(box[:, None], fin=0, fout=0.05), True, "synthesised: F#4 369.99 Hz, 5 inharmonic partials (1, 2, 3.01, 5.93, 16.6), exp decay tau 0.42 s, tine click")
# Mirror ring: thin glass, wine-glass-like inharmonic partials with detuned twins (3-6 Hz beats) for shimmer.
for i, f0 in enumerate([1318.5, 1661.2], 1):
    ring = partials(f0, [(1, 1.0, 0.55, 3.1 + i), (2.32, 0.35, 0.35, 4.7), (4.25, 0.18, 0.18, 6.0), (6.63, 0.08, 0.09, 0)],
                    1.8, 0.006)
    write(f"mirror_ring_{i}", fades(ring[:, None], fin=0, fout=0.15), True,
          f"synthesised: {f0} Hz glass, partials 1/2.32/4.25/6.63, detuned twins, exp decay tau 0.55 s")
print("\n".join(report))
