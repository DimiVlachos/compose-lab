package dev.dimvlachos.lab.core.presentation.components.snowglobe

/**
 * The globe's glass, run over the layer the interior is drawn into. In the sphere around [center]
 * of [radius] (both in the layer's pixels) each pixel looks through a water lens: it reads the
 * interior closer to the middle the nearer it is to the rim, with the red and blue taps pulled a
 * little apart, then gains a fresnel sheen at the edge and a soft window reflection up and to the
 * left. Written in the shared subset of AGSL and SkSL, so both platforms compile the same text.
 */
internal const val GlassShaderSource =
    """
uniform shader content;
uniform float2 center;
uniform float radius;

half4 main(float2 p) {
    float2 d = (p - center) / radius;
    float r2 = dot(d, d);
    if (r2 >= 1.0) {
        return content.eval(p);
    }
    float z = sqrt(1.0 - r2);
    float k = 0.70 + 0.30 * z;
    float2 base = center + d * k * radius;
    float2 spread = d * (1.0 - z) * radius * 0.014;
    half4 mid = content.eval(base);
    half r = content.eval(base + spread).r;
    half b = content.eval(base - spread).b;
    half3 col = half3(r, mid.g, b);

    float fres = 0.04 + 0.96 * pow(1.0 - z, 5.0);
    col = mix(col, half3(0.92, 0.95, 1.0), half(fres * 0.55));

    float2 w = (d - float2(-0.36, -0.44)) * float2(1.0, 1.7);
    float window = exp(-dot(w, w) * 26.0);
    float2 v = (d - float2(0.42, 0.30)) * float2(1.6, 1.0);
    float back = exp(-dot(v, v) * 40.0) * 0.25;
    col += half3(window * 0.55 + back);

    return half4(col, 1.0);
}
"""
