#!/usr/bin/env python3
"""Generate catalog-style SVG illustrations for every product of the shop.

Usage: python3 scripts/generate-product-images.py [--out DIR]

Default output dir: frontend/public/images/products (relative to the repo root).
Stdlib only, deterministic, re-runnable (files are overwritten).
The product list is embedded below (sku|image_url|brand|category slug|name);
it was exported from the DB with:
  select p.sku, p.image_url, b.name, c.slug, p.name from products p
  join brands b on b.id=p.brand_id join categories c on c.id=p.category_id order by p.id
"""
import argparse
import hashlib
import math
import os
import re
import sys
from xml.sax.saxutils import escape

PRODUCTS_DATA = """
NGK-ILKAR7B11|/images/products/ngk-ilkar7b11.svg|NGK|spark-plugs|Свеча зажигания иридиевая NGK ILKAR7B11
NGK-BKR6E|/images/products/ngk-bkr6e.svg|NGK|spark-plugs|Свеча зажигания NGK BKR6E
BOS-FR7DC|/images/products/bos-fr7dc.svg|Bosch|spark-plugs|Свеча зажигания Bosch Super Plus FR7DC+
GAT-K015578XS|/images/products/gat-k015578xs.svg|Gates|timing-belts|Комплект ремня ГРМ Gates K015578XS
GAT-5539XS|/images/products/gat-5539xs.svg|Gates|timing-belts|Ремень ГРМ Gates 5539XS
MANN-W712/95|/images/products/mann-w712-95.svg|MANN-FILTER|oil-filters|Фильтр масляный MANN-FILTER W 712/95
MANN-W68/3|/images/products/mann-w68-3.svg|MANN-FILTER|oil-filters|Фильтр масляный MANN-FILTER W 68/3
BOS-F026407157|/images/products/bos-f026407157.svg|Bosch|oil-filters|Фильтр масляный Bosch F 026 407 157
MANN-C27009|/images/products/mann-c27009.svg|MANN-FILTER|air-filters|Фильтр воздушный MANN-FILTER C 27 009
MANN-C26017|/images/products/mann-c26017.svg|MANN-FILTER|air-filters|Фильтр воздушный MANN-FILTER C 26 017
BOS-F026400385|/images/products/bos-f026400385.svg|Bosch|air-filters|Фильтр воздушный Bosch F 026 400 385
MANN-CUK26009|/images/products/mann-cuk26009.svg|MANN-FILTER|cabin-filters|Фильтр салонный угольный MANN-FILTER CUK 26 009
MANN-CU2939|/images/products/mann-cu2939.svg|MANN-FILTER|cabin-filters|Фильтр салонный MANN-FILTER CU 29 39
BOS-1987435094|/images/products/bos-1987435094.svg|Bosch|cabin-filters|Фильтр салонный Bosch 1 987 435 094
BRE-P83167|/images/products/bre-p83167.svg|Brembo|brake-pads|Колодки тормозные передние Brembo P 83 167
TRW-GDB1550|/images/products/trw-gdb1550.svg|TRW|brake-pads|Колодки тормозные передние TRW GDB1550
BOS-0986494818|/images/products/bos-0986494818.svg|Bosch|brake-pads|Колодки тормозные передние Bosch 0 986 494 818
BRE-P68061|/images/products/bre-p68061.svg|Brembo|brake-pads|Колодки тормозные передние Brembo P 68 061
BRE-09A71311|/images/products/bre-09a71311.svg|Brembo|brake-discs|Диск тормозной передний Brembo 09.A713.11
TRW-DF4823|/images/products/trw-df4823.svg|TRW|brake-discs|Диск тормозной передний TRW DF4823
KYB-339394|/images/products/kyb-339394.svg|KYB|shock-absorbers|Амортизатор передний правый KYB Excel-G 339394
KYB-334834|/images/products/kyb-334834.svg|KYB|shock-absorbers|Амортизатор передний KYB Excel-G 334834
KYB-3340046|/images/products/kyb-3340046.svg|KYB|shock-absorbers|Амортизатор передний KYB 3340046
LEM-3446901|/images/products/lem-3446901.svg|Lemförder|stabilizer-links|Стойка стабилизатора передняя Lemförder 34469 01
LEM-3891601|/images/products/lem-3891601.svg|Lemförder|stabilizer-links|Стойка стабилизатора передняя Lemförder 38916 01
VAR-560408054|/images/products/var-560408054.svg|Varta|batteries|Аккумулятор Varta Blue Dynamic E11 74 А·ч
BOS-0092S40050|/images/products/bos-0092s40050.svg|Bosch|batteries|Аккумулятор Bosch S4 005 60 А·ч
PHI-12972PRC1|/images/products/phi-12972prc1.svg|Philips|bulbs|Лампа H7 Philips Vision +30%
PHI-12342PRC1|/images/products/phi-12342prc1.svg|Philips|bulbs|Лампа H4 Philips Vision +30%
CAS-15F6A1|/images/products/cas-15f6a1.svg|Castrol|motor-oils|Масло моторное Castrol EDGE 5W-30 LL 4 л
CAS-15A4CF|/images/products/cas-15a4cf.svg|Castrol|motor-oils|Масло моторное Castrol Magnatec 5W-40 4 л
FEL-430206026|/images/products/fel-430206026.svg|Felix|antifreeze|Антифриз Felix Carbox G12+ красный 5 кг
DEN-IK20TT|/images/products/den-ik20tt.svg|Denso|spark-plugs|Свеча зажигания иридиевая Denso IK20TT
DEN-K20TT|/images/products/den-k20tt.svg|Denso|spark-plugs|Свеча зажигания Denso K20TT
NGK-LKR7DIX11S|/images/products/ngk-lkr7dix11s.svg|NGK|spark-plugs|Свеча зажигания иридиевая NGK LKR7DIX-11S
BOS-FR7KPP33|/images/products/bos-fr7kpp33.svg|Bosch|spark-plugs|Свеча зажигания Bosch Platinum FR7KPP33+
NGK-PLZKAR6A11|/images/products/ngk-plzkar6a11.svg|NGK|spark-plugs|Свеча зажигания NGK Laser Platinum PLZKAR6A-11
GAT-K015499XS|/images/products/gat-k015499xs.svg|Gates|timing-belts|Комплект ремня ГРМ Gates K015499XS
CON-CT1028K2|/images/products/con-ct1028k2.svg|ContiTech|timing-belts|Комплект ремня ГРМ ContiTech CT1028K2
CON-CT1167|/images/products/con-ct1167.svg|ContiTech|timing-belts|Ремень ГРМ ContiTech CT1167
FEB-24419|/images/products/feb-24419.svg|Febi Bilstein|timing-belts|Ролик натяжной ремня ГРМ Febi 24419
GAT-T41250|/images/products/gat-t41250.svg|Gates|timing-belts|Ролик натяжной ГРМ Gates T41250
MAH-OC593/4|/images/products/mah-oc593-4.svg|MAHLE|oil-filters|Фильтр масляный MAHLE OC 593/4
MAH-OC1196|/images/products/mah-oc1196.svg|MAHLE|oil-filters|Фильтр масляный MAHLE OC 1196
MANN-W75/3|/images/products/mann-w75-3.svg|MANN-FILTER|oil-filters|Фильтр масляный MANN-FILTER W 75/3
BOS-F026407210|/images/products/bos-f026407210.svg|Bosch|oil-filters|Фильтр масляный Bosch F 026 407 210
MANN-HU7020Z|/images/products/mann-hu7020z.svg|MANN-FILTER|oil-filters|Фильтр масляный MANN-FILTER HU 7020 z
MAH-LX1780/3|/images/products/mah-lx1780-3.svg|MAHLE|air-filters|Фильтр воздушный MAHLE LX 1780/3
MANN-C1858|/images/products/mann-c1858.svg|MANN-FILTER|air-filters|Фильтр воздушный MANN-FILTER C 1858
BOS-F026400539|/images/products/bos-f026400539.svg|Bosch|air-filters|Фильтр воздушный Bosch F 026 400 539
MANN-C24005|/images/products/mann-c24005.svg|MANN-FILTER|air-filters|Фильтр воздушный MANN-FILTER C 24 005
MAH-LX3778|/images/products/mah-lx3778.svg|MAHLE|air-filters|Фильтр воздушный MAHLE LX 3778
MANN-CUK2939|/images/products/mann-cuk2939.svg|MANN-FILTER|cabin-filters|Фильтр салонный угольный MANN-FILTER CUK 29 39
MAH-LAK875|/images/products/mah-lak875.svg|MAHLE|cabin-filters|Фильтр салонный угольный MAHLE LAK 875
BOS-1987432598|/images/products/bos-1987432598.svg|Bosch|cabin-filters|Фильтр салонный Bosch 1 987 432 598
DEN-DCC1009|/images/products/den-dcc1009.svg|Denso|cabin-filters|Фильтр салонный Denso DCC-1009
MANN-CUK25001|/images/products/mann-cuk25001.svg|MANN-FILTER|cabin-filters|Фильтр салонный угольный MANN-FILTER CUK 25 001
FEN-BP43136|/images/products/fen-bp43136.svg|Fenox|brake-pads|Колодки тормозные передние Fenox BP43136
FEN-BP43049|/images/products/fen-bp43049.svg|Fenox|brake-pads|Колодки тормозные передние Fenox BP43049
TRW-GDB2069|/images/products/trw-gdb2069.svg|TRW|brake-pads|Колодки тормозные передние TRW GDB2069
BRE-P85075|/images/products/bre-p85075.svg|Brembo|brake-pads|Колодки тормозные задние Brembo P 85 075
TRW-GDB1949|/images/products/trw-gdb1949.svg|TRW|brake-pads|Колодки тормозные передние TRW GDB1949
FEN-BP53007|/images/products/fen-bp53007.svg|Fenox|brake-pads|Колодки тормозные задние барабанные Fenox BP53007
BOS-0986494833|/images/products/bos-0986494833.svg|Bosch|brake-pads|Колодки тормозные передние Bosch 0 986 494 833
FEN-TB219020|/images/products/fen-tb219020.svg|Fenox|brake-discs|Диск тормозной передний Fenox TB219020
FEN-TB2181|/images/products/fen-tb2181.svg|Fenox|brake-discs|Диск тормозной передний Fenox TB2181
BRE-09A85811|/images/products/bre-09a85811.svg|Brembo|brake-discs|Диск тормозной задний Brembo 08.A858.11
TRW-DF6911|/images/products/trw-df6911.svg|TRW|brake-discs|Диск тормозной передний TRW DF6911
BRE-09D52813|/images/products/bre-09d52813.svg|Brembo|brake-discs|Диск тормозной передний Brembo 09.D528.13
FEB-171345|/images/products/feb-171345.svg|Febi Bilstein|brake-discs|Диск тормозной передний Febi 171345
SAC-315976|/images/products/sac-315976.svg|SACHS|shock-absorbers|Амортизатор передний SACHS 315 976
KYB-3348052|/images/products/kyb-3348052.svg|KYB|shock-absorbers|Амортизатор задний KYB Excel-G 3348052
MON-G16376|/images/products/mon-g16376.svg|Monroe|shock-absorbers|Амортизатор передний Monroe G16376
KYB-339788|/images/products/kyb-339788.svg|KYB|shock-absorbers|Амортизатор передний KYB Excel-G 339788
SAC-317592|/images/products/sac-317592.svg|SACHS|shock-absorbers|Амортизатор задний SACHS 317 592
FEN-A61268|/images/products/fen-a61268.svg|Fenox|shock-absorbers|Стойка амортизатора передняя Fenox A61268
KYB-3348101|/images/products/kyb-3348101.svg|KYB|shock-absorbers|Амортизатор задний KYB Excel-G 3348101
FEB-19518|/images/products/feb-19518.svg|Febi Bilstein|stabilizer-links|Стойка стабилизатора передняя Febi 19518
LEM-3826201|/images/products/lem-3826201.svg|Lemförder|stabilizer-links|Стойка стабилизатора передняя Lemförder 38262 01
FEN-LS20125|/images/products/fen-ls20125.svg|Fenox|stabilizer-links|Стойка стабилизатора передняя Fenox LS20125
FEB-48003|/images/products/feb-48003.svg|Febi Bilstein|stabilizer-links|Стойка стабилизатора передняя Febi 48003
LEM-3784701|/images/products/lem-3784701.svg|Lemförder|stabilizer-links|Стойка стабилизатора передняя Lemförder 37847 01
VAR-574012068|/images/products/var-574012068.svg|Varta|batteries|Аккумулятор Varta Blue Dynamic E43 72 А·ч
EXI-EA640|/images/products/exi-ea640.svg|Exide|batteries|Аккумулятор Exide Premium EA640 64 А·ч
BOS-0092S50080|/images/products/bos-0092s50080.svg|Bosch|batteries|Аккумулятор Bosch S5 008 77 А·ч
VAR-570901076|/images/products/var-570901076.svg|Varta|batteries|Аккумулятор Varta Silver Dynamic AGM E39 70 А·ч
EXI-EB740|/images/products/exi-eb740.svg|Exide|batteries|Аккумулятор Exide Premium EB740 74 А·ч
OSR-64210NBS|/images/products/osr-64210nbs.svg|OSRAM|bulbs|Лампа H7 OSRAM Night Breaker Silver
OSR-64193NL|/images/products/osr-64193nl.svg|OSRAM|bulbs|Лампа H4 OSRAM Night Breaker Laser
PHI-12258PRC1|/images/products/phi-12258prc1.svg|Philips|bulbs|Лампа H1 Philips Vision +30%
OSR-66340CBN|/images/products/osr-66340cbn.svg|OSRAM|bulbs|Лампа ксеноновая D3S OSRAM Xenarc Cool Blue
PHI-11362ULWX2|/images/products/phi-11362ulwx2.svg|Philips|bulbs|Лампы светодиодные H11 Philips Ultinon Pro6000 (2 шт.)
OSR-7506|/images/products/osr-7506.svg|OSRAM|bulbs|Лампа P21W OSRAM Original
LM-8065|/images/products/lm-8065.svg|Liqui Moly|motor-oils|Масло моторное Liqui Moly Special Tec AA 5W-30 4 л
SHE-550046288|/images/products/she-550046288.svg|Shell|motor-oils|Масло моторное Shell Helix HX8 5W-40 4 л
CAS-15C4EE|/images/products/cas-15c4ee.svg|Castrol|motor-oils|Масло моторное Castrol GTX 10W-40 4 л
LM-39041|/images/products/lm-39041.svg|Liqui Moly|motor-oils|Масло моторное Liqui Moly Top Tec 4200 5W-30 5 л
SHE-550048036|/images/products/she-550048036.svg|Shell|motor-oils|Масло моторное Shell Helix Ultra 0W-20 4 л
FEL-430206034|/images/products/fel-430206034.svg|Felix|antifreeze|Антифриз Felix Prolonger G11 зелёный 5 кг
LM-21149|/images/products/lm-21149.svg|Liqui Moly|antifreeze|Антифриз Liqui Moly KFS 12++ концентрат 1 л
FEL-430206065|/images/products/fel-430206065.svg|Felix|antifreeze|Антифриз Felix Energy G12+ концентрат 1 кг
VAL-698870|/images/products/val-698870.svg|Valeo|cabin-filters|Фильтр салонный угольный Valeo 698870
"""

FONT = "Inter, Arial, sans-serif"
W, H = 800, 600

# brand -> (primary, secondary, text colour on primary)
PALETTE = {
    "Bosch": ("#E20015", "#1F2A44", "#FFFFFF"),
    "MANN-FILTER": ("#00843D", "#FFFFFF", "#FFFFFF"),
    "MAHLE": ("#0057A6", "#E8EEF6", "#FFFFFF"),
    "NGK": ("#D7141A", "#FFFFFF", "#FFFFFF"),
    "Denso": ("#DC0032", "#FFFFFF", "#FFFFFF"),
    "Brembo": ("#D50000", "#1A1A1A", "#FFFFFF"),
    "TRW": ("#0055A5", "#FFFFFF", "#FFFFFF"),
    "Fenox": ("#F28C00", "#222222", "#FFFFFF"),
    "KYB": ("#FFD200", "#C8102E", "#1A1A1A"),
    "SACHS": ("#F07D00", "#1F1F1F", "#FFFFFF"),
    "Monroe": ("#FFCC00", "#0A2A66", "#0A2A66"),
    "Lemförder": ("#003A70", "#FFFFFF", "#FFFFFF"),
    "Febi Bilstein": ("#FFD400", "#1A1A1A", "#1A1A1A"),
    "Gates": ("#D71920", "#111111", "#FFFFFF"),
    "ContiTech": ("#FFB400", "#111111", "#111111"),
    "Varta": ("#0061AE", "#FFD100", "#FFFFFF"),
    "Exide": ("#E30613", "#1A1A1A", "#FFFFFF"),
    "Philips": ("#0B5ED7", "#FFFFFF", "#FFFFFF"),
    "OSRAM": ("#FF6600", "#FFFFFF", "#FFFFFF"),
    "Castrol": ("#009A44", "#E2001A", "#FFFFFF"),
    "Shell": ("#FFD500", "#DD1D21", "#DD1D21"),
    "Liqui Moly": ("#003D7C", "#E30613", "#FFFFFF"),
    "Felix": ("#D52B1E", "#1A1A1A", "#FFFFFF"),
    "Valeo": ("#3AAA35", "#FFFFFF", "#FFFFFF"),
}
DEFAULT_COLORS = ("#4A5568", "#E2E8F0", "#FFFFFF")


# ---------------------------------------------------------------- helpers

def parse_products():
    items = []
    for line in PRODUCTS_DATA.strip().splitlines():
        line = line.strip()
        if not line:
            continue
        sku, url, brand, slug, name = line.split("|", 4)
        items.append({"sku": sku, "url": url, "brand": brand, "slug": slug, "name": name})
    return items


def f(v):
    """Compact number formatting."""
    s = ("%.1f" % v).rstrip("0").rstrip(".")
    return "0" if s == "-0" else s


def _rgb(c):
    c = c.lstrip("#")
    return [int(c[i:i + 2], 16) for i in (0, 2, 4)]


def shade(c, k):
    """k>0 lightens towards white, k<0 darkens towards black (k in -1..1)."""
    r = _rgb(c)
    if k >= 0:
        r = [x + (255 - x) * k for x in r]
    else:
        r = [x * (1 + k) for x in r]
    return "#%02X%02X%02X" % tuple(max(0, min(255, int(round(x)))) for x in r)


def is_light(c):
    r, g, b = _rgb(c)
    return 0.299 * r + 0.587 * g + 0.114 * b > 165


def colors(brand):
    return PALETTE.get(brand, DEFAULT_COLORS)


def seed(sku, mod):
    return int(hashlib.md5(sku.encode("utf-8")).hexdigest(), 16) % mod


def text_width(s, size):
    w = 0.0
    for ch in s:
        if ch == " ":
            w += 0.28
        elif ch in "il.,:;|!'·":
            w += 0.3
        elif ch in "MWМШЩЖЮmwмшщжю":
            w += 0.9
        elif ch.isupper() or ch.isdigit():
            w += 0.72
        else:
            w += 0.58
    return w * size


def fit(s, max_w, size, min_size=8):
    """Largest font size <= size so that s fits into max_w."""
    w = text_width(s, size)
    if w > max_w:
        size = max(min_size, size * max_w / w)
    return size


def text(x, y, s, size, fill, weight=700, anchor="middle", max_w=None, extra=""):
    if max_w:
        size = fit(s, max_w, size)
    tl = ""
    if max_w and text_width(s, size) > max_w:  # hit min size: squeeze
        tl = ' textLength="%s" lengthAdjust="spacingAndGlyphs"' % f(max_w)
    return ('<text x="%s" y="%s" font-family="%s" font-size="%s" font-weight="%d" '
            'fill="%s" text-anchor="%s"%s%s>%s</text>') % (
        f(x), f(y), FONT, f(size), weight, fill, anchor, tl, extra, escape(s))


def lin(id_, stops, x1=0, y1=0, x2=1, y2=0):
    st = "".join('<stop offset="%s" stop-color="%s"%s/>' % (
        f(o), c, (' stop-opacity="%s"' % f(a)) if a is not None else "")
        for o, c, a in [(s + (None,))[:3] for s in stops])
    return '<linearGradient id="%s" x1="%s" y1="%s" x2="%s" y2="%s">%s</linearGradient>' % (
        id_, f(x1), f(y1), f(x2), f(y2), st)


def rad(id_, stops, cx=0.5, cy=0.5, r=0.5):
    st = "".join('<stop offset="%s" stop-color="%s"%s/>' % (
        f(o), c, (' stop-opacity="%s"' % f(a)) if a is not None else "")
        for o, c, a in [(s + (None,))[:3] for s in stops])
    return '<radialGradient id="%s" cx="%s" cy="%s" r="%s">%s</radialGradient>' % (
        id_, f(cx), f(cy), f(r), st)


def metal_h(id_, base="#9AA1AA"):
    """Horizontal cylinder-like metal gradient."""
    return lin(id_, [(0, shade(base, -0.45)), (0.22, shade(base, 0.55)),
                     (0.45, shade(base, 0.15)), (0.8, shade(base, -0.2)),
                     (1, shade(base, -0.5))])


def metal_v(id_, base="#9AA1AA"):
    return lin(id_, [(0, shade(base, -0.4)), (0.3, shade(base, 0.6)),
                     (0.6, shade(base, 0.05)), (1, shade(base, -0.5))], 0, 0, 0, 1)


def color_h(id_, c):
    k = 0.5 if is_light(c) else 1.0
    return lin(id_, [(0, shade(c, -0.45 * k)), (0.25, shade(c, 0.25)),
                     (0.5, c), (0.85, shade(c, -0.25 * k)), (1, shade(c, -0.5 * k))])


def poly(points, fill, extra=""):
    return '<polygon points="%s" fill="%s"%s/>' % (
        " ".join("%s,%s" % (f(x), f(y)) for x, y in points), fill, extra)


def lerp(a, b, t):
    return (a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t)


def shadow(cx, cy, rx, ry, op=0.35):
    return ('<ellipse cx="%s" cy="%s" rx="%s" ry="%s" fill="url(#shd)" opacity="%s"/>'
            % (f(cx), f(cy), f(rx), f(ry), f(op)))


def badge(brand):
    pri, _, txt = colors(brand)
    size = 17
    w = text_width(brand, size) + 28
    return ('<g><rect x="24" y="24" width="%s" height="34" rx="17" fill="%s"/>%s</g>' % (
        f(w), pri, text(24 + w / 2, 47, brand, size, txt)))


def wrap_svg(p, defs, body):
    bg = rad("bg", [(0, "#FFFFFF"), (0.6, "#F3F4F6"), (1, "#DDE1E6")], 0.5, 0.42, 0.75)
    shd = rad("shd", [(0, "#000000", 0.55), (0.6, "#000000", 0.18), (1, "#000000", 0)])
    title = escape(p["name"])
    return ('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 %d %d" width="%d" height="%d" '
            'role="img" aria-label="%s"><title>%s</title><defs>%s%s%s</defs>'
            '<rect width="%d" height="%d" fill="url(#bg)"/>%s%s</svg>\n') % (
        W, H, W, H, title, title, bg, shd, "".join(defs), W, H, body, badge(p["brand"]))


def model_line(p):
    """Product series text: name without the generic prefix and brand."""
    name = p["name"]
    i = name.find(p["brand"])
    if i < 0 and p["brand"] == "Febi Bilstein":
        i = name.find("Febi")
        if i >= 0:
            return name[i + 4:].strip()
    return name[i + len(p["brand"]):].strip() if i >= 0 else p["sku"]


# ---------------------------------------------------------------- spark plug

def draw_spark_plug(p):
    pri, sec, txt = colors(p["brand"])
    tilt = -14 + seed(p["sku"], 7) - 3
    iridium = "иридиев" in p["name"] or "Platinum" in p["name"]
    d = [metal_h("m", "#A7AEB7"), metal_h("mg", "#C9A94B" if iridium else "#B0B6BE"),
         lin("cer", [(0, "#C9CED6"), (0.3, "#FFFFFF"), (0.65, "#EEF0F3"), (1, "#B8BEC7")]),
         metal_h("hex", "#8E959E")]
    b = [shadow(400, 512, 170, 22)]
    g = []
    # terminal
    g.append('<rect x="386" y="62" width="28" height="34" rx="6" fill="url(#m)"/>')
    g.append('<rect x="380" y="92" width="40" height="10" rx="3" fill="url(#m)"/>')
    # insulator with ribs
    g.append('<rect x="371" y="100" width="58" height="150" rx="8" fill="url(#cer)"/>')
    for i in range(5):
        y = 110 + i * 15
        g.append('<rect x="365" y="%d" width="70" height="9" rx="4.5" fill="url(#cer)" '
                 'stroke="#B9BFC8" stroke-width="0.8"/>' % y)
    # brand print (vertical) on insulator
    g.append(poly([(371, 244), (429, 244), (440, 262), (360, 262)], "url(#cer)"))
    g.append('<g transform="translate(400 213) rotate(-90)">%s</g>' % text(
        0, 6, p["brand"].upper(), 17, pri if not is_light(pri) else sec, max_w=54))
    # hex
    g.append('<rect x="350" y="260" width="100" height="52" rx="3" fill="url(#hex)"/>')
    g.append('<rect x="378" y="260" width="44" height="52" fill="#FFFFFF" opacity="0.18"/>')
    g.append('<path d="M378 260v52M422 260v52" stroke="#5E646C" stroke-width="1.2"/>')
    g.append('<rect x="362" y="312" width="76" height="14" fill="url(#m)"/>')
    g.append('<rect x="355" y="326" width="90" height="9" rx="2" fill="url(#hex)"/>')
    # threaded body
    g.append('<rect x="368" y="335" width="64" height="100" fill="url(#m)"/>')
    th = "".join("M368 %s L432 %s" % (f(y), f(y + 5)) for y in range(340, 432, 7))
    g.append('<path d="%s" stroke="#5B6168" stroke-width="2" opacity="0.65"/>' % th)
    g.append('<rect x="370" y="435" width="60" height="6" fill="#6B7178"/>')
    # electrodes
    g.append('<rect x="396" y="440" width="8" height="18" fill="url(#mg)"/>')
    g.append('<path d="M422 438 V472 Q422 478 414 478 H390" fill="none" stroke="#7D838B" '
             'stroke-width="8" stroke-linecap="round"/>')
    # brand ring on hex
    g.append('<rect x="350" y="300" width="100" height="5" fill="%s" opacity="0.85"/>' % pri)
    b.append('<g transform="rotate(%d 400 290)">%s</g>' % (tilt, "".join(g)))
    return d, "".join(b)


# ---------------------------------------------------------------- timing belt

def roller(cx, cy, r, pri, idn):
    return "".join([
        '<circle cx="%s" cy="%s" r="%s" fill="#000" opacity="0.18"/>' % (f(cx + 6), f(cy + 8), f(r)),
        '<circle cx="%s" cy="%s" r="%s" fill="url(#%s)"/>' % (f(cx), f(cy), f(r), idn),
        '<circle cx="%s" cy="%s" r="%s" fill="none" stroke="#FFFFFF" stroke-opacity="0.6" stroke-width="2"/>'
        % (f(cx), f(cy), f(r * 0.92)),
        '<circle cx="%s" cy="%s" r="%s" fill="#3A3F45"/>' % (f(cx), f(cy), f(r * 0.6)),
        '<circle cx="%s" cy="%s" r="%s" fill="%s"/>' % (f(cx), f(cy), f(r * 0.52), pri),
        '<circle cx="%s" cy="%s" r="%s" fill="url(#rh)"/>' % (f(cx), f(cy), f(r * 0.36)),
        '<circle cx="%s" cy="%s" r="%s" fill="#2B2F34"/>' % (f(cx), f(cy), f(r * 0.14)),
    ])


def draw_timing_belt(p):
    pri, sec, txt = colors(p["brand"])
    name = p["name"]
    is_kit = "Комплект" in name
    is_roller = name.startswith("Ролик")
    d = [rad("rl", [(0, "#F4F6F8"), (0.7, "#C3C9D0"), (0.86, "#E9ECEF"), (1, "#7E858D")]),
         rad("rh", [(0, "#FFFFFF"), (0.7, "#A9B0B8"), (1, "#6E757D")])]
    b = [shadow(400, 500, 280, 26)]
    if is_roller:
        xa, xb, cy, r = 230, 420, 240, 110
    else:
        xa, xb, cy, r = 280, 520, 260, 130

    def stadium(rr):
        return "M%s %s H%s A%s %s 0 0 1 %s %s H%s A%s %s 0 0 1 %s %sZ" % (
            f(xa), f(cy - rr), f(xb), f(rr), f(rr), f(xb), f(cy + rr), f(xa), f(rr), f(rr), f(xa), f(cy - rr))
    # belt: dark band, teeth on the inner side, thin outer edge
    b.append('<path d="%s" fill="none" stroke="#000" stroke-opacity="0.15" stroke-width="32" '
             'transform="translate(6 10)"/>' % stadium(r))
    b.append('<path d="%s" fill="none" stroke="#24272B" stroke-width="30"/>' % stadium(r))
    b.append('<path d="%s" fill="none" stroke="#3B4046" stroke-width="10" stroke-dasharray="9 7"/>'
             % stadium(r - 19))
    b.append('<path d="%s" fill="none" stroke="#50565D" stroke-width="2"/>' % stadium(r + 13))
    label = "%s  %s" % (p["brand"].upper(), p["sku"].split("-", 1)[1])
    b.append(text((xa + xb) / 2, cy - r + 6, label, 16, "#E6E8EB", max_w=xb - xa - 30,
                  extra=' letter-spacing="1"'))
    if is_roller:
        b.append(roller(500, 330, 118, pri, "rl"))
    elif is_kit:
        b.append(roller(300, 380, 82, pri, "rl"))
        b.append(roller(505, 395, 64, pri, "rl"))
    else:
        b.append(roller(540, 390, 70, pri, "rl"))
    return d, "".join(b)


# ---------------------------------------------------------------- oil filter

def draw_oil_filter(p):
    pri, sec, txt = colors(p["brand"])
    d = [color_h("body", pri), metal_h("m", "#B4BAC2"),
         rad("top", [(0, "#EEF1F4"), (0.75, "#B9C0C8"), (1, "#8A929B")])]
    x1, x2, top, bot, rx, ry = 285, 515, 170, 445, 115, 28
    cx = 400
    b = [shadow(400, 478, 175, 26)]
    b.append('<path d="M%d %d V%d A%d %d 0 0 0 %d %d V%d Z" fill="url(#body)"/>' % (
        x1, top, bot, rx, ry, x2, bot, top))
    # seam band near base plate
    b.append('<path d="M%d %d V%d A%d %d 0 0 0 %d %d V%d A%d %d 0 0 1 %d %d Z" fill="url(#m)"/>' % (
        x1, top, top + 18, rx, ry, x2, top + 18, top, rx, ry, x1, top))
    # label
    b.append('<rect x="300" y="250" width="200" height="118" rx="10" fill="#FFFFFF" opacity="0.96"/>')
    b.append('<rect x="300" y="250" width="200" height="12" rx="6" fill="%s" opacity="0.25"/>' % pri)
    b.append(text(400, 312, p["brand"].upper(), 34, pri if not is_light(pri) else "#1A1A1A", max_w=176))
    b.append(text(400, 346, model_line(p), 18, "#3A3F45", 600, max_w=176))
    # highlight
    b.append('<rect x="318" y="190" width="14" height="250" rx="7" fill="#FFFFFF" opacity="0.28"/>')
    # base plate (top)
    b.append('<ellipse cx="%d" cy="%d" rx="%d" ry="%d" fill="url(#top)"/>' % (cx, top, rx, ry))
    b.append('<ellipse cx="%d" cy="%d" rx="%d" ry="%d" fill="none" stroke="#1F2226" stroke-width="9"/>'
             % (cx, top, rx - 18, ry - 5))
    for i in range(8):
        a = math.pi * 2 * i / 8 + 0.2
        b.append('<ellipse cx="%s" cy="%s" rx="7" ry="3" fill="#3C4248"/>' % (
            f(cx + 55 * math.cos(a)), f(top + 13 * math.sin(a))))
    b.append('<ellipse cx="%d" cy="%d" rx="30" ry="8" fill="#9AA1AA"/>' % (cx, top))
    b.append('<ellipse cx="%d" cy="%d" rx="24" ry="6" fill="#2F3439"/>' % (cx, top + 1))
    b.append('<ellipse cx="%d" cy="%d" rx="18" ry="4" fill="none" stroke="#6A7178" stroke-width="1.5"/>'
             % (cx, top + 1))
    return d, "".join(b)


# ---------------------------------------------------------------- panel filters

def draw_panel_filter(p, cabin):
    pri, sec, txt = colors(p["brand"])
    charcoal = "угольный" in p["name"]
    if cabin:
        A, B, C, D = (165, 255), (495, 200), (640, 285), (310, 345)
        depth = 24
        frame_top, frame_side = "#E4E6E9", "#B9BDC3"
        media = ("#4A4E53", "#2C2F33") if charcoal else ("#FAFAF6", "#D6D6CC")
        inset, n = 0.035, 34
    else:
        A, B, C, D = (160, 245), (500, 185), (650, 285), (310, 350)
        depth = 46
        frame_top, frame_side = "#34383D", "#1F2226"
        media = ("#F7EFD2", "#D5C79A")
        inset, n = 0.07, 26
    dn = lambda pt: (pt[0], pt[1] + depth)
    b = [shadow(410, 365 + depth, 270, 40, 0.3)]
    # side faces
    b.append(poly([A, D, dn(D), dn(A)], shade(frame_side, -0.1)))
    b.append(poly([D, C, dn(C), dn(D)], frame_side))
    # top frame
    b.append(poly([A, B, C, D], frame_top))
    # media area
    iA = lerp(lerp(A, B, inset), lerp(D, C, inset), inset * 1.6)
    iB = lerp(lerp(A, B, 1 - inset), lerp(D, C, 1 - inset), inset * 1.6)
    iC = lerp(lerp(A, B, 1 - inset), lerp(D, C, 1 - inset), 1 - inset * 1.6)
    iD = lerp(lerp(A, B, inset), lerp(D, C, inset), 1 - inset * 1.6)
    for i in range(n):
        t0, t1, tm = i / n, (i + 1) / n, (i + 0.5) / n
        p0, p1, pm = lerp(iA, iB, t0), lerp(iA, iB, t1), lerp(iA, iB, tm)
        q0, q1, qm = lerp(iD, iC, t0), lerp(iD, iC, t1), lerp(iD, iC, tm)
        b.append(poly([p0, pm, qm, q0], media[0]))
        b.append(poly([pm, p1, q1, qm], media[1]))
    # inner shadow line along the frame
    b.append('<polygon points="%s" fill="none" stroke="#000" stroke-opacity="0.25" stroke-width="2"/>' %
             " ".join("%s,%s" % (f(x), f(y)) for x, y in (iA, iB, iC, iD)))
    # label on the front face
    L = math.hypot(C[0] - D[0], C[1] - D[1])
    ux, uy = (C[0] - D[0]) / L, (C[1] - D[1]) / L
    lw = L * 0.62
    lh = depth - 8 if not cabin else depth - 4
    lab = ['<rect x="%s" y="%s" width="%s" height="%s" rx="3" fill="%s"/>' % (
        f((L - lw) / 2), f((depth - lh) / 2), f(lw), f(lh), pri)]
    lab.append(text(L / 2, depth / 2 + lh * 0.3, "%s  %s" % (p["brand"].upper(), model_line(p)),
                    lh * 0.72, txt, max_w=lw - 14))
    b.append('<g transform="matrix(%s %s 0 1 %s %s)">%s</g>' % (
        "%.4f" % ux, "%.4f" % uy, f(D[0]), f(D[1]), "".join(lab)))
    # top highlight edge
    b.append('<path d="M%s %s L%s %s" stroke="#FFFFFF" stroke-opacity="0.35" stroke-width="2"/>' % (
        f(A[0]), f(A[1]), f(B[0]), f(B[1])))
    return [], "".join(b)


# ---------------------------------------------------------------- brake pads

def sector(cx, cy, r1, r2, a1, a2):
    def pt(r, a):
        a = math.radians(a)
        return cx + r * math.sin(a), cy - r * math.cos(a)
    p1, p2, p3, p4 = pt(r2, a1), pt(r2, a2), pt(r1, a2), pt(r1, a1)
    return "M%s %s A%s %s 0 0 1 %s %s L%s %s A%s %s 0 0 0 %s %s Z" % (
        f(p1[0]), f(p1[1]), f(r2), f(r2), f(p2[0]), f(p2[1]),
        f(p3[0]), f(p3[1]), f(r1), f(r1), f(p4[0]), f(p4[1]))


def pad(dx, dy, pri, txt, brand, back, idp):
    cx, cy = 400 + dx, 600 + dy
    g = []
    plate = sector(cx, cy, 225, 340, -38, 38)
    g.append('<path d="%s" fill="#000" opacity="0.18" transform="translate(8 12)"/>' % plate)
    # plate thickness
    g.append('<path d="%s" fill="#4B5057" transform="translate(0 9)"/>' % plate)
    if back:
        g.append('<path d="%s" fill="url(#%s)"/>' % (plate, idp))
        g.append('<path d="%s" fill="none" stroke="#FFFFFF" stroke-opacity="0.3" stroke-width="3"/>'
                 % sector(cx, cy, 238, 327, -33, 33))
        g.append(text(cx, cy - 268, brand.upper(), 34, txt, max_w=200))
        for a in (-33, 33):
            x = cx + 300 * math.sin(math.radians(a))
            y = cy - 300 * math.cos(math.radians(a))
            g.append('<circle cx="%s" cy="%s" r="9" fill="#2E3237"/>' % (f(x), f(y)))
    else:
        g.append('<path d="%s" fill="url(#pl)"/>' % plate)
        fr = sector(cx, cy, 236, 330, -31, 31)
        g.append('<path d="%s" fill="#1E2023" transform="translate(0 10)"/>' % fr)
        g.append('<path d="%s" fill="url(#fric)"/>' % fr)
        # groove
        g.append('<path d="M%s %s L%s %s" stroke="#15171A" stroke-width="5"/>' % (
            f(cx), f(cy - 236), f(cx), f(cy - 330)))
    return "".join(g)


def draw_brake_pads(p):
    pri, sec, txt = colors(p["brand"])
    d = [lin("bp", [(0, shade(pri, 0.2)), (0.5, pri), (1, shade(pri, -0.35))], 0, 0, 0, 1),
         metal_v("pl", "#8F969F"),
         lin("fric", [(0, "#5A5E63"), (0.45, "#3D4045"), (1, "#26282B")], 0, 0, 0.3, 1)]
    b = [shadow(410, 505, 270, 30)]
    b.append(pad(-55, -10, pri, txt, p["brand"], True, "bp"))
    b.append(pad(60, 80, pri, txt, p["brand"], False, "bp"))
    return d, "".join(b)


# ---------------------------------------------------------------- brake disc

def draw_brake_disc(p):
    pri, sec, txt = colors(p["brand"])
    cx, cy, rx, ry, th = 400, 270, 240, 150, 44
    d = [lin("face", [(0, "#8C939C"), (0.3, "#E4E7EB"), (0.5, "#B3B9C1"),
                      (0.75, "#D9DDE2"), (1, "#7F868F")], 0, 0, 1, 0.4),
         lin("edge", [(0, "#5F656D"), (0.5, "#A0A7AF"), (1, "#4E545B")]),
         lin("hat", [(0, "#3A3E44"), (0.5, "#5E646B"), (1, "#2A2D31")], 0, 0, 1, 1),
         rad("bore", [(0, "#0F1113"), (1, "#3A3F45")])]
    b = [shadow(cx, cy + ry + th + 18, 270, 34)]
    b.append('<ellipse cx="%d" cy="%d" rx="%d" ry="%d" fill="#3E434A"/>' % (cx, cy + th, rx, ry))
    b.append('<rect x="%d" y="%d" width="%d" height="%d" fill="url(#edge)"/>' % (cx - rx, cy, 2 * rx, th))
    b.append('<path d="M%d %d A%d %d 0 0 0 %d %d V%d A%d %d 0 0 1 %d %d Z" fill="url(#edge)"/>' % (
        cx - rx, cy, rx, ry, cx + rx, cy, cy + th, rx, ry, cx - rx, cy + th))
    # vents on the edge
    vents = []
    n = 30
    for k in range(1, n):
        phi = math.pi * k / n
        x = cx + rx * math.cos(phi)
        y = cy + ry * math.sin(phi)
        wv = max(1.2, 7 * math.sin(phi))
        vents.append('<path d="M%s %s v%s" stroke="#1C1F22" stroke-width="%s"/>' % (
            f(x), f(y + 13), f(th - 24), f(wv)))
    b.append("".join(vents))
    # friction face
    b.append('<ellipse cx="%d" cy="%d" rx="%d" ry="%d" fill="url(#face)"/>' % (cx, cy, rx, ry))
    for r in (0.93, 0.83, 0.72):
        b.append('<ellipse cx="%d" cy="%d" rx="%s" ry="%s" fill="none" stroke="#6E757D" '
                 'stroke-opacity="0.35" stroke-width="1.2"/>' % (cx, cy, f(rx * r), f(ry * r)))
    b.append('<path d="M%s %s A%d %d 0 0 1 %s %s" fill="none" stroke="#FFFFFF" stroke-opacity="0.55" '
             'stroke-width="4"/>' % (f(cx - rx * 0.86), f(cy - ry * 0.5), rx, ry,
                                      f(cx - rx * 0.1), f(cy - ry * 0.99)))
    # hat (raised hub)
    hrx, hry, hh = 112, 70, 26
    hy = cy - hh
    b.append('<ellipse cx="%d" cy="%d" rx="%d" ry="%d" fill="#000" opacity="0.25"/>' % (
        cx + 6, cy + 6, hrx + 4, hry + 3))
    b.append('<path d="M%d %d A%d %d 0 0 0 %d %d V%d A%d %d 0 0 1 %d %d Z" fill="#2E3237"/>' % (
        cx - hrx, hy, hrx, hry, cx + hrx, hy, cy, hrx, hry, cx - hrx, cy))
    b.append('<ellipse cx="%d" cy="%d" rx="%d" ry="%d" fill="url(#hat)"/>' % (cx, hy, hrx, hry))
    b.append('<ellipse cx="%d" cy="%d" rx="%d" ry="%d" fill="none" stroke="%s" stroke-width="4"/>' % (
        cx, hy, hrx - 6, hry - 4, pri))
    for i in range(5):
        a = -math.pi / 2 + 2 * math.pi * i / 5
        b.append('<ellipse cx="%s" cy="%s" rx="10" ry="6.5" fill="#101214"/>' % (
            f(cx + 70 * math.cos(a)), f(hy + 44 * math.sin(a))))
    b.append('<ellipse cx="%d" cy="%d" rx="36" ry="23" fill="url(#bore)"/>' % (cx, hy))
    b.append('<ellipse cx="%d" cy="%d" rx="36" ry="23" fill="none" stroke="#8F969F" stroke-width="2"/>'
             % (cx, hy))
    return d, "".join(b)


# ---------------------------------------------------------------- shock absorber

def draw_shock(p):
    pri, sec, txt = colors(p["brand"])
    name = p["name"]
    rear = "задн" in name
    strut = "Стойка" in name
    tilt = (6 if rear else -6) + seed(p["sku"], 5) - 2
    body_c = pri
    d = [color_h("body", body_c), metal_h("rod", "#C8CDD3"), metal_h("m", "#9AA1AA"),
         lin("boot", [(0, "#16181A"), (0.3, "#4A4E54"), (1, "#121315")])]
    b = [shadow(400, 535, 150, 20)]
    g = []
    if rear:
        # top eye
        g.append('<rect x="392" y="96" width="16" height="40" fill="url(#rod)"/>')
        g.append('<circle cx="400" cy="86" r="30" fill="url(#m)"/>')
        g.append('<circle cx="400" cy="86" r="20" fill="#1E2023"/>')
        g.append('<circle cx="400" cy="86" r="9" fill="url(#rod)"/>')
        g.append('<rect x="390" y="130" width="20" height="80" fill="url(#rod)"/>')
        g.append('<rect x="372" y="132" width="56" height="14" rx="4" fill="url(#m)"/>')
    else:
        # threaded stud + nut, rod, bump stop
        g.append('<rect x="393" y="58" width="14" height="40" fill="url(#rod)"/>')
        g.append('<path d="%s" stroke="#6F767E" stroke-width="1.5"/>' % "".join(
            "M393 %d l14 3" % y for y in range(62, 96, 5)))
        g.append('<rect x="383" y="96" width="34" height="18" rx="2" fill="url(#m)"/>')
        g.append('<rect x="390" y="114" width="20" height="100" fill="url(#rod)"/>')
        # dust boot (corrugated)
        boot = []
        for i in range(7):
            y = 128 + i * 12
            boot.append('<rect x="378" y="%d" width="44" height="10" rx="5" fill="url(#boot)"/>' % y)
        g.append("".join(boot))
    # main cylinder
    top_y, bot_y = 205, 455 if not strut else 430
    g.append('<rect x="362" y="%d" width="76" height="%d" rx="6" fill="url(#body)"/>' % (top_y, bot_y - top_y))
    g.append('<rect x="358" y="%d" width="84" height="16" rx="5" fill="url(#m)"/>' % (top_y - 6))
    g.append('<rect x="376" y="%d" width="8" height="%d" rx="4" fill="#FFFFFF" opacity="0.3"/>' % (
        top_y + 20, bot_y - top_y - 40))
    # vertical brand print
    lab_c = txt
    ly0, ly1 = (top_y + 20, bot_y - 20) if rear else (278, bot_y - 18)
    g.append('<g transform="translate(410 %d) rotate(-90)">%s</g>' % (
        (ly0 + ly1) // 2, text(0, 0, p["brand"].upper(), 28, lab_c, max_w=ly1 - ly0)))
    if not rear:
        # spring seat
        sy = 255
        g.append('<ellipse cx="400" cy="%d" rx="74" ry="14" fill="#2E3237"/>' % (sy + 5))
        g.append('<ellipse cx="400" cy="%d" rx="74" ry="14" fill="url(#m)"/>' % sy)
        g.append('<ellipse cx="400" cy="%d" rx="40" ry="8" fill="url(#body)"/>' % sy)
    if strut or not rear:
        # knuckle bracket
        by = bot_y - 10
        g.append('<path d="M366 %d H434 L446 %d V%d H354 V%d Z" fill="url(#m)"/>' % (
            by, by + 30, by + 92, by + 30))
        g.append('<circle cx="400" cy="%d" r="9" fill="#202326"/><circle cx="400" cy="%d" r="9" fill="#202326"/>'
                 % (by + 46, by + 76))
        g.append('<rect x="354" y="%d" width="92" height="6" fill="#FFFFFF" opacity="0.25"/>' % (by + 30))
    else:
        g.append('<rect x="388" y="%d" width="24" height="24" fill="url(#m)"/>' % bot_y)
        g.append('<circle cx="400" cy="%d" r="30" fill="url(#m)"/>' % (bot_y + 40))
        g.append('<circle cx="400" cy="%d" r="20" fill="#1E2023"/>' % (bot_y + 40))
        g.append('<circle cx="400" cy="%d" r="9" fill="url(#rod)"/>' % (bot_y + 40))
    b.append('<g transform="rotate(%d 400 300)">%s</g>' % (tilt, "".join(g)))
    return d, "".join(b)


# ---------------------------------------------------------------- stabilizer link

def ball_end(x, up, flip):
    """Ball joint housing at x (on rod axis y=300) with stud pointing up/down."""
    s = -1 if up else 1
    g = []
    # boot
    y0 = 300 + s * 22
    g.append(poly([(x - 30, y0), (x + 30, y0), (x + 16, y0 + s * 38), (x - 16, y0 + s * 38)], "url(#boot)"))
    for k in (12, 24):
        g.append('<path d="M%s %s h%s" stroke="#5A5F66" stroke-width="2"/>' % (
            f(x - 30 + k * 0.4), f(y0 + s * k), f(60 - k * 0.8)))
    # stud
    ys = y0 + s * 38
    ty = ys if s > 0 else ys - 62
    g.append('<rect x="%s" y="%s" width="18" height="62" fill="url(#rod)"/>' % (f(x - 9), f(ty)))
    g.append('<path d="%s" stroke="#6B7178" stroke-width="1.6"/>' % "".join(
        "M%s %s l18 3" % (f(x - 9), f(ty + 22 + j * 5)) for j in range(8)))
    # nut
    ny = ys + s * 14 - (10 if s > 0 else 10)
    g.append('<rect x="%s" y="%s" width="40" height="20" rx="2" fill="url(#m)"/>' % (f(x - 20), f(ny)))
    g.append('<rect x="%s" y="%s" width="14" height="20" fill="#FFFFFF" opacity="0.25"/>' % (f(x - 7), f(ny)))
    # housing
    g.append('<circle cx="%s" cy="300" r="36" fill="url(#hs)"/>' % f(x))
    g.append('<circle cx="%s" cy="300" r="36" fill="none" stroke="#2B2F34" stroke-width="2"/>' % f(x))
    g.append('<circle cx="%s" cy="300" r="12" fill="#2B2F34" opacity="0.6"/>' % f(x))
    return "".join(g)


def draw_stab_link(p):
    pri, sec, txt = colors(p["brand"])
    tilt = -18 + seed(p["sku"], 9) - 4
    same_side = seed(p["sku"] + "s", 2) == 0
    d = [metal_v("rodh", "#A9B0B8"), metal_h("rod", "#C8CDD3"), metal_h("m", "#9AA1AA"),
         lin("boot", [(0, "#16181A"), (0.35, "#4A4E54"), (1, "#121315")]),
         rad("hs", [(0, "#E9ECEF"), (0.7, "#8E959E"), (1, "#5D636B")], 0.38, 0.35, 0.7),
         color_h("lab", pri)]
    b = [shadow(400, 470, 260, 24)]
    g = []
    g.append('<rect x="200" y="290" width="400" height="20" rx="10" fill="url(#rodh)"/>')
    g.append('<rect x="200" y="294" width="400" height="4" fill="#FFFFFF" opacity="0.5"/>')
    g.append('<rect x="315" y="283" width="170" height="34" rx="6" fill="%s"/>' % pri)
    g.append('<rect x="315" y="285" width="170" height="8" rx="4" fill="#FFFFFF" opacity="0.25"/>')
    g.append(text(400, 306, p["brand"].upper(), 18, txt, max_w=154))
    g.append(ball_end(200, True, False))
    g.append(ball_end(600, same_side, True))
    b.append('<g transform="rotate(%d 400 300)">%s</g>' % (tilt, "".join(g)))
    return d, "".join(b)


# ---------------------------------------------------------------- battery

def draw_battery(p):
    pri, sec, txt = colors(p["brand"])
    m = re.search(r"(\d+)\s*А·ч", p["name"])
    cap = (m.group(1) + " А·ч") if m else ""
    series = model_line(p)
    series = re.sub(r"\s*\d+\s*А·ч", "", series).strip()
    d = [lin("lab", [(0, shade(pri, 0.15)), (1, shade(pri, -0.3))], 0, 0, 0, 1),
         lin("top", [(0, "#4A4F56"), (1, "#2C3035")], 0, 0, 0, 1),
         metal_h("term", "#B8BEC6")]
    x0, x1, y0, y1, ox, oy = 205, 545, 235, 470, 70, -45
    b = [shadow(410, 478, 260, 30)]
    # side
    b.append(poly([(x1, y0), (x1 + ox, y0 + oy), (x1 + ox, y1 + oy), (x1, y1)], "#1E2125"))
    # top
    b.append(poly([(x0, y0), (x1, y0), (x1 + ox, y0 + oy), (x0 + ox, y0 + oy)], "url(#top)"))
    b.append(poly([(x0 + 18, y0 - 6), (x1 - 10, y0 - 6), (x1 + ox - 22, y0 + oy + 6),
                   (x0 + ox + 4, y0 + oy + 6)], "#3A3F45"))
    # handle (lying flat)
    b.append('<path d="M%d %d q130 -24 250 -14" fill="none" stroke="#17191C" stroke-width="7" '
             'stroke-linecap="round"/>' % (x0 + 70, y0 - 20))
    # terminals
    for tx, ty, sign, col in ((x0 + 58, y0 - 14, "+", "#D62828"), (x1 - 12, y0 - 14, "−", "#1F4E9C")):
        b.append('<ellipse cx="%d" cy="%d" rx="20" ry="8" fill="%s"/>' % (tx, ty + 2, col))
        b.append('<rect x="%d" y="%d" width="24" height="26" fill="url(#term)"/>' % (tx - 12, ty - 26))
        b.append('<ellipse cx="%d" cy="%d" rx="12" ry="5" fill="#E3E6EA"/>' % (tx, ty - 26))
        b.append(text(tx + (34 if sign == "+" else -34), ty + 2, sign, 26, col))
    # front
    b.append('<rect x="%d" y="%d" width="%d" height="%d" fill="#26292E"/>' % (x0, y0, x1 - x0, y1 - y0))
    b.append('<rect x="%d" y="%d" width="%d" height="%d" rx="6" fill="url(#lab)"/>' % (
        x0 + 16, y0 + 18, x1 - x0 - 32, y1 - y0 - 50))
    b.append('<rect x="%d" y="%d" width="%d" height="10" fill="%s"/>' % (x0 + 16, y0 + 128, x1 - x0 - 32, sec))
    b.append(text((x0 + x1) / 2, y0 + 78, p["brand"].upper(), 48, txt, max_w=x1 - x0 - 70))
    b.append(text((x0 + x1) / 2, y0 + 112, series, 20, txt, 600, max_w=x1 - x0 - 70))
    if cap:
        b.append(text((x0 + x1) / 2, y0 + 172, cap, 30, txt, max_w=200))
    b.append('<rect x="%d" y="%d" width="%d" height="14" fill="#1A1C1F"/>' % (x0, y1 - 14, x1 - x0))
    return d, "".join(b)


# ---------------------------------------------------------------- bulbs

def halogen(cx, s, tint, cap_col, xenon):
    g = []
    t = lambda v: f(v)
    # base / connector
    g.append('<rect x="%s" y="%s" width="%s" height="%s" rx="6" fill="%s"/>' % (
        t(cx - 34 * s), t(355), t(68 * s), t(80), "#E9E4D6" if xenon else "#2C2F33"))
    g.append('<rect x="%s" y="435" width="%s" height="34" fill="url(#m)"/>' % (t(cx - 22 * s), t(10 * s)))
    g.append('<rect x="%s" y="435" width="%s" height="34" fill="url(#m)"/>' % (t(cx + 12 * s), t(10 * s)))
    # collar + flange
    g.append('<rect x="%s" y="322" width="%s" height="36" fill="url(#m)"/>' % (t(cx - 38 * s), t(76 * s)))
    g.append('<rect x="%s" y="306" width="%s" height="18" rx="4" fill="url(#m)"/>' % (t(cx - 78 * s), t(156 * s)))
    g.append('<rect x="%s" y="306" width="%s" height="5" fill="#FFFFFF" opacity="0.5"/>' % (t(cx - 74 * s), t(148 * s)))
    # glass
    gx, gw = cx - 34 * s, 68 * s
    g.append('<rect x="%s" y="120" width="%s" height="190" rx="%s" fill="url(#glass)" stroke="#9AA3AD" '
             'stroke-opacity="0.6" stroke-width="1.5"/>' % (t(gx), t(gw), t(32 * s)))
    if tint:
        g.append('<rect x="%s" y="120" width="%s" height="190" rx="%s" fill="%s" opacity="0.18"/>' % (
            t(gx), t(gw), t(32 * s), tint))
    # internals
    if xenon:
        g.append('<rect x="%s" y="170" width="%s" height="90" rx="6" fill="#EEF2F7" stroke="#B7C0CA"/>' % (
            t(cx - 8 * s), t(16 * s)))
        g.append('<ellipse cx="%s" cy="215" rx="%s" ry="10" fill="#FFFFFF" stroke="#9AA3AD"/>' % (
            t(cx), t(11 * s)))
    else:
        g.append('<path d="M%s 300 V206 M%s 300 V170" stroke="#7C848D" stroke-width="2.5" fill="none"/>' % (
            t(cx - 14 * s), t(cx + 14 * s)))
        coil = "M%s 206 " % t(cx - 14 * s) + " ".join(
            "l%s %s" % (t(3.5 * s), (-6 if i % 2 == 0 else 6)) for i in range(8))
        g.append('<path d="%s" stroke="#5A6068" stroke-width="2" fill="none"/>' % coil)
        g.append('<path d="M%s 170 H%s V206" stroke="#7C848D" stroke-width="2.5" fill="none"/>' % (
            t(cx + 14 * s), t(cx + 14 * s)))
    # cap on tip
    g.append('<path d="M%s 152 V140 a%s %s 0 0 1 %s 0 V152 Z" fill="%s"/>' % (
        t(gx + 6 * s), t(28 * s), t(20), t(56 * s), cap_col))
    g.append('<rect x="%s" y="134" width="%s" height="150" rx="%s" fill="#FFFFFF" opacity="0.55"/>' % (
        t(gx + 10 * s), t(8 * s), t(4 * s)))
    return "".join(g)


def led(cx, s):
    g = []
    t = f
    g.append('<rect x="%s" y="352" width="%s" height="100" rx="8" fill="url(#m)"/>' % (t(cx - 40 * s), t(80 * s)))
    for i in range(6):
        g.append('<rect x="%s" y="%d" width="%s" height="8" fill="#5A6068"/>' % (
            t(cx - 40 * s), 362 + i * 15, t(80 * s)))
    g.append('<rect x="%s" y="452" width="%s" height="20" rx="4" fill="#2C2F33"/>' % (t(cx - 24 * s), t(48 * s)))
    g.append('<rect x="%s" y="306" width="%s" height="18" rx="4" fill="url(#m)"/>' % (t(cx - 70 * s), t(140 * s)))
    g.append('<rect x="%s" y="324" width="%s" height="30" fill="#2C2F33"/>' % (t(cx - 30 * s), t(60 * s)))
    g.append('<rect x="%s" y="140" width="%s" height="168" rx="6" fill="url(#m)"/>' % (t(cx - 16 * s), t(32 * s)))
    g.append('<rect x="%s" y="130" width="%s" height="14" rx="5" fill="#9AA1AA"/>' % (t(cx - 13 * s), t(26 * s)))
    for y in (175, 215, 255):
        g.append('<rect x="%s" y="%d" width="%s" height="22" rx="2" fill="#FFE9A8" stroke="#D8B24A"/>' % (
            t(cx - 9 * s), y, t(18 * s)))
    return "".join(g)


def p21w(cx):
    g = []
    g.append('<rect x="%s" y="290" width="76" height="120" rx="6" fill="url(#m)"/>' % f(cx - 38))
    g.append('<path d="%s" stroke="#7C848D" stroke-width="2"/>' % "".join(
        "M%s %d h76" % (f(cx - 38), y) for y in (300, 400)))
    g.append('<circle cx="%s" cy="340" r="7" fill="#7C848D"/>' % f(cx - 38))
    g.append('<rect x="%s" y="410" width="44" height="30" fill="#E9E4D6"/>' % f(cx - 22))
    g.append('<ellipse cx="%s" cy="446" rx="12" ry="6" fill="url(#m)"/>' % f(cx))
    g.append('<circle cx="%s" cy="210" r="88" fill="url(#glass)" stroke="#9AA3AD" stroke-opacity="0.6" '
             'stroke-width="1.5"/>' % f(cx))
    g.append('<path d="M%s 290 V215 M%s 290 V215" stroke="#7C848D" stroke-width="2.5"/>' % (f(cx - 20), f(cx + 20)))
    coil = "M%s 215 " % f(cx - 20) + " ".join("l5 %d" % (-6 if i % 2 == 0 else 6) for i in range(8))
    g.append('<path d="%s" stroke="#5A6068" stroke-width="2" fill="none"/>' % coil)
    g.append('<path d="M%s 160 a70 70 0 0 1 50 -26" stroke="#FFFFFF" stroke-width="10" stroke-linecap="round" '
             'opacity="0.6" fill="none"/>' % f(cx - 58))
    return "".join(g)


def draw_bulb(p):
    pri, sec, txt = colors(p["brand"])
    name = p["name"]
    d = [metal_h("m", "#B5BBC3"),
         lin("glass", [(0, "#DCE3EA", 0.9), (0.35, "#FFFFFF", 0.6), (0.7, "#E8EEF4", 0.7),
                       (1, "#B9C4CF", 0.9)])]
    b = [shadow(400, 490, 170, 22)]
    m = re.search(r"\b(H\d+|D\d+S|P21W)\b", name)
    code = m.group(1) if m else ""
    if "P21W" in name:
        b.append('<g transform="translate(400 470) scale(1.12) translate(-400 -470)">%s</g>' % p21w(400))
    elif "светодиод" in name:
        b.append(led(320, 1))
        b.append(led(480, 1))
        b[0] = shadow(400, 490, 230, 22)
    else:
        xenon = "ксенон" in name
        tint = "#3B82F6" if ("Blue" in name or "Laser" in name) else (
            "#8A97A8" if "Silver" in name else None)
        cap_col = "#2B6CB0" if "Laser" in name else ("#9AA1AA" if "Silver" in name else "#3A3E44")
        b.append('<g transform="translate(400 470) scale(1.18) translate(-400 -470)">%s</g>'
                 % halogen(400, 1, tint, cap_col, xenon))
    if code:
        # type tag in brand colour
        w = text_width(code, 30) + 30
        b.append('<rect x="%s" y="%s" width="%s" height="46" rx="10" fill="%s"/>' % (
            f(W - 40 - w), f(H - 86), f(w), pri))
        b.append(text(W - 40 - w / 2, H - 52, code, 30, txt))
    return d, "".join(b)


# ---------------------------------------------------------------- canisters

def canister_shape(x0, y0, w, h):
    """Front outline of a 4-5 l canister; returns (body path, handle path, cap x)."""
    x1, y1 = x0 + w, y0 + h
    body = ("M%s %s V%s Q%s %s %s %s H%s Q%s %s %s %s V%s Q%s %s %s %s H%s Q%s %s %s %s Z" % (
        f(x0), f(y1 - 22), f(y0 + 36), f(x0), f(y0), f(x0 + 36), f(y0),
        f(x1 - 36), f(x1), f(y0), f(x1), f(y0 + 36), f(y1 - 22),
        f(x1), f(y1), f(x1 - 22), f(y1), f(x0 + 22), f(x0), f(y1), f(x0), f(y1 - 22)))
    handle = "M%s %s L%s %s Q%s %s %s %s H%s Q%s %s %s %s L%s %s" % (
        f(x1 - 150), f(y0 + 4), f(x1 - 140), f(y0 - 62), f(x1 - 136), f(y0 - 78), f(x1 - 118), f(y0 - 78),
        f(x1 - 52), f(x1 - 34), f(y0 - 78), f(x1 - 32), f(y0 - 62), f(x1 - 26), f(y0 + 4))
    return body, handle


def draw_canister(p, antifreeze):
    pri, sec, txt = colors(p["brand"])
    name = p["name"]
    vm = re.search(r"(\d+)\s*(л|кг)", name)
    vol = vm.group(0) if vm else ""
    small = vm and vm.group(1) == "1"
    if antifreeze:
        if "++" in name or "KFS" in name:
            liq = "#C2389E"
        elif "G11" in name or "зел" in name:
            liq = "#2FB54A"
        else:
            liq = "#E0283A"
    d = []
    b = []
    if small:
        x0, y0, w, h = 290, 165, 220, 315
    else:
        x0, y0, w, h = 245, 185, 310, 300
    if "5 л" in name and not antifreeze:
        x0, y0, w, h = 235, 175, 330, 310
    x1, y1 = x0 + w, y0 + h
    b.append(shadow((x0 + x1) / 2, y1 + 10, w * 0.62, 24))
    if small:
        pts = [("M", x0, y1 - 20), ("V", y0 + 70), ("Q", x0, y0 + 25, x0 + 50, y0 + 15),
               ("L", x0 + 60, y0 + 5), ("V", y0 - 10), ("H", x1 - 60), ("V", y0 + 5),
               ("L", x1 - 50, y0 + 15), ("Q", x1, y0 + 25, x1, y0 + 70), ("V", y1 - 20),
               ("Q", x1, y1, x1 - 20, y1), ("H", x0 + 20), ("Q", x0, y1, x0, y1 - 20)]
        body = "".join(c[0] + " ".join(f(v) for v in c[1:]) for c in pts) + "Z"
        handle = None
        cap = (x0 + 52, y0 - 48, w - 104, 40)
    else:
        body, handle = canister_shape(x0, y0, w, h)
        cap = (x0 + 30, y0 - 46, 64, 40)
    if antifreeze:
        d.append(lin("liq", [(0, shade(liq, -0.25)), (0.4, shade(liq, 0.15)), (1, shade(liq, -0.35))]))
        d.append(lin("pl", [(0, "#FFFFFF", 0.12), (0.3, "#FFFFFF", 0.45), (1, "#E5E9EE", 0.15)]))
        d.append('<clipPath id="cb"><path d="%s"/></clipPath>' % body)
        level = y0 + h * (0.22 if not small else 0.32)
        b.append('<path d="%s" fill="#F2F5F8" opacity="0.8"/>' % body)
        b.append('<g clip-path="url(#cb)"><rect x="%s" y="%s" width="%s" height="%s" fill="url(#liq)"/>'
                 '<rect x="%s" y="%s" width="%s" height="6" fill="%s"/></g>' % (
                     f(x0), f(level), f(w), f(y1 - level + 2), f(x0), f(level), f(w), shade(liq, 0.35)))
        b.append('<path d="%s" fill="url(#pl)" stroke="#AEB6BF" stroke-width="2"/>' % body)
        if handle:
            b.append('<path d="%s" fill="none" stroke="#C9D0D7" stroke-width="30" stroke-linejoin="round" '
                     'opacity="0.9"/>' % handle)
            b.append('<path d="%s" fill="none" stroke="#FFFFFF" stroke-width="22" stroke-linejoin="round" '
                     'opacity="0.7"/>' % handle)
        cap_col = "#1F2226" if liq != "#2FB54A" else "#1E6B30"
        lab_fill, lab_txt = pri, txt
    else:
        d.append(color_h("cb", pri))
        if handle:
            b.append('<path d="%s" fill="none" stroke="%s" stroke-width="30" stroke-linejoin="round"/>' % (
                handle, shade(pri, -0.15)))
            b.append('<path d="%s" fill="none" stroke="#FFFFFF" stroke-opacity="0.25" stroke-width="6" '
                     'stroke-linejoin="round" transform="translate(-6 -4)"/>' % handle)
        b.append('<path d="%s" fill="url(#cb)"/>' % body)
        cap_col = sec if not is_light(sec) or sec == "#FFFFFF" else shade(sec, -0.2)
        if cap_col.upper() in ("#FFFFFF", "#E8EEF6"):
            cap_col = "#1F2226"
        lab_fill = "#FFFFFF" if not is_light(pri) else "#1F2226"
        lab_txt = pri if not is_light(pri) else pri
        if is_light(pri):
            lab_fill, lab_txt = "#FFFFFF", sec
    # cap + neck
    cx_, cy_, cw, ch = cap
    b.append('<rect x="%s" y="%s" width="%s" height="14" fill="%s"/>' % (
        f(cx_ + 6), f(cy_ + ch - 4), f(cw - 12), shade(cap_col, 0.2)))
    b.append('<rect x="%s" y="%s" width="%s" height="%s" rx="5" fill="%s"/>' % (
        f(cx_), f(cy_), f(cw), f(ch), cap_col))
    b.append('<path d="%s" stroke="#FFFFFF" stroke-opacity="0.25" stroke-width="3"/>' % "".join(
        "M%s %s v%s" % (f(cx_ + 8 + i * 8), f(cy_ + 6), f(ch - 12)) for i in range(int((cw - 10) // 8))))
    # label
    if antifreeze:
        lx0, lx1 = x0 + (52 if not small else 18), x1 - (52 if not small else 18)
        ly0 = y0 + (100 if not small else 95)
        ly1 = y1 - 34
    else:
        lx0, lx1 = x0 + 24, x1 - 24
        ly0 = y0 + (60 if not small else 70)
        ly1 = y1 - 26
    lw = lx1 - lx0
    b.append('<rect x="%s" y="%s" width="%s" height="%s" rx="12" fill="%s"%s/>' % (
        f(lx0), f(ly0), f(lw), f(ly1 - ly0), lab_fill,
        ' stroke="#FFFFFF" stroke-width="4"' if antifreeze else ""))
    if not antifreeze:
        b.append('<rect x="%s" y="%s" width="%s" height="10" fill="%s"/>' % (
            f(lx0), f(ly0 + 64), f(lw), sec if sec != lab_fill else pri))
    cxm = (lx0 + lx1) / 2
    line = model_line(p)
    vis = re.search(r"\d+W-\d+", line)
    visc = vis.group(0) if vis else ""
    if visc:
        series = line[:vis.start()].strip()
    else:
        series = re.sub(r"\s*\d+\s*(л|кг)\s*$", "", line)
        series = re.sub(r"\s*(красный|зелёный|концентрат)", "", series).strip()
    oy_brand, oy_series = (42, 84) if antifreeze else (50, 108)
    b.append(text(cxm, ly0 + oy_brand, p["brand"].upper(), 34 if not small else 28, lab_txt, max_w=lw - 30))
    b.append(text(cxm, ly0 + oy_series, series, 26 if not small else 20,
                  "#1F2226" if lab_fill == "#FFFFFF" else txt, 700, max_w=lw - 30))
    if visc:
        b.append(text(cxm, ly0 + 158, visc, 40, lab_txt, 800, max_w=lw - 30))
    elif antifreeze:
        cw_ = "красный" if liq == "#E0283A" else ("зелёный" if liq == "#2FB54A" else "фиолетовый")
        if "концентрат" in name:
            cw_ = "концентрат"
        b.append(text(cxm, ly0 + 116, cw_, 20 if not small else 16,
                      "#1F2226" if lab_fill == "#FFFFFF" else txt, 600, max_w=lw - 30))
    if vol:
        b.append(text(cxm, ly1 - 16, vol, 24 if not small else 20,
                      "#1F2226" if lab_fill == "#FFFFFF" else txt, 700, max_w=lw - 30))
    # highlight
    b.append('<rect x="%s" y="%s" width="10" height="%s" rx="5" fill="#FFFFFF" opacity="0.35"/>' % (
        f(x0 + 9), f(y0 + 50), f(h - 90)))
    return d, "".join(b)


# ---------------------------------------------------------------- dispatcher

DRAWERS = {
    "spark-plugs": draw_spark_plug,
    "timing-belts": draw_timing_belt,
    "oil-filters": draw_oil_filter,
    "air-filters": lambda p: draw_panel_filter(p, False),
    "cabin-filters": lambda p: draw_panel_filter(p, True),
    "brake-pads": draw_brake_pads,
    "brake-discs": draw_brake_disc,
    "shock-absorbers": draw_shock,
    "stabilizer-links": draw_stab_link,
    "batteries": draw_battery,
    "bulbs": draw_bulb,
    "motor-oils": lambda p: draw_canister(p, False),
    "antifreeze": lambda p: draw_canister(p, True),
}


def render(p):
    drawer = DRAWERS.get(p["slug"])
    if drawer is None:
        raise SystemExit("no drawing template for category %r (sku %s)" % (p["slug"], p["sku"]))
    defs, body = drawer(p)
    return wrap_svg(p, defs, body)


def main():
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    ap = argparse.ArgumentParser(description="Generate product SVG illustrations.")
    ap.add_argument("--out", default=os.path.join(root, "frontend", "public", "images", "products"))
    args = ap.parse_args()
    os.makedirs(args.out, exist_ok=True)
    products = parse_products()
    for p in products:
        fname = os.path.basename(p["url"])
        with open(os.path.join(args.out, fname), "w", encoding="utf-8", newline="\n") as fh:
            fh.write(render(p))
    print("wrote %d files to %s" % (len(products), args.out))


if __name__ == "__main__":
    sys.exit(main())
