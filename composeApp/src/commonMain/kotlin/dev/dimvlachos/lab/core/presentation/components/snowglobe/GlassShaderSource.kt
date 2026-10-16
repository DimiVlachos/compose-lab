package dev.dimvlachos.lab.core.presentation.components.snowglobe

/**
 * The globe's glass: the prototype's fragment shader `FS`, line for line, run over the layer the
 * interior is drawn into. In the sphere around [center] of [radius] (the layer's pixels; `p` is
 * y-down, as the prototype's `vP` is) each pixel refracts through a water lens, the three colours
 * at slightly different indices, and gains the room's reflection by fresnel, the window it faces, a
 * specular glint, the rim light, the caustic crescent on the inner edge, the wall's thickness and
 * the air bubble at the top. Outside the sphere it leaves nothing. Written in the shared subset of
 * AGSL and SkSL, so both platforms compile the same text.
 */
internal const val GlassShaderSource =
    """
uniform shader content;
uniform float2 center;
uniform float radius;
uniform float uPx;
uniform float uWinW;
uniform float uWinH;
uniform float uMull;
uniform float3 uL;
uniform float3 uEnvTop;
uniform float3 uEnvBot;
uniform float3 uWin;
uniform float3 uCaus;
uniform float3 uRim;
uniform float2 uBub;

float3 tap(float2 p, float z, float eta) {
    float3 n = float3(p, z);
    float3 t = refract(float3(0.0, 0.0, -1.0), n, eta);
    float s = z / max(-t.z, 0.08);
    float2 h = p + t.xy * s;
    return content.eval(center + h * radius).rgb;
}

half4 main(float2 fragCoord) {
    float2 p = (fragCoord - center) / radius;
    float r2 = dot(p, p);
    float r = sqrt(r2);
    float a = clamp((1.0 - r) / (1.6 * uPx), 0.0, 1.0);
    if (a <= 0.0) {
        return half4(0.0);
    }
    float z = sqrt(max(1.0 - r2, 0.0004));
    float3 col;
    col.r = tap(p, z, 0.7505).r;
    col.g = tap(p, z, 0.7478).g;
    col.b = tap(p, z, 0.7440).b;
    col *= mix(float3(1.0), float3(0.80, 0.90, 0.97), smoothstep(0.35, 1.0, r2));
    float3 n = float3(p, z);
    float3 R = reflect(float3(0.0, 0.0, -1.0), n);
    float3 env = mix(uEnvBot, uEnvTop, smoothstep(0.35, -0.55, R.y));
    float3 ex = normalize(cross(uL, float3(0.0, 1.0, 0.0)));
    float3 ey = cross(ex, uL);
    float u = dot(R, ex);
    float v = dot(R, ey);
    float f = dot(R, uL);
    float win = smoothstep(uWinW, uWinW * 0.55, abs(u)) * smoothstep(uWinH, uWinH * 0.6, abs(v)) * step(0.0, f);
    float mull = max(smoothstep(0.02, 0.009, abs(u)), smoothstep(0.02, 0.009, abs(v + uWinH * 0.1)));
    win *= 1.0 - 0.75 * mull * uMull;
    float F = 0.04 + 0.96 * pow(1.0 - z, 5.0);
    float3 c = col * (1.0 - F) + env * F + uWin * win * (0.07 + 0.93 * F);
    float sp = pow(max(dot(R, normalize(uL + float3(0.07, -0.05, 0.0))), 0.0), 700.0);
    c += float3(1.0) * sp * 1.5;
    c += uRim * 0.16 * smoothstep(0.78, 0.97, r) * smoothstep(0.1, 0.8, p.x) * smoothstep(0.75, -0.2, abs(p.y));
    float2 Ld = normalize(uL.xy);
    float side = max(dot(p / max(r, 0.001), -Ld), 0.0);
    c += uCaus * pow(side, 2.5) * smoothstep(0.6, 0.9, r) * smoothstep(0.985, 0.93, r);
    float wall = smoothstep(0.962, 0.992, r);
    c = mix(c, c * 0.72 + env * 0.3, wall * 0.7);
    c += uRim * 0.55 * smoothstep(0.976, 0.99, r) * smoothstep(1.0, 0.992, r);
    float2 bq = (p - uBub) * float2(1.0, 1.7);
    float bd = length(bq) / 0.06;
    if (bd < 1.2) {
        float inside = smoothstep(1.0, 0.9, bd);
        c = mix(c, c * 0.6 + float3(0.42, 0.46, 0.5), inside * 0.55);
        float ring = smoothstep(0.74, 0.96, bd) * smoothstep(1.06, 0.97, bd);
        c = mix(c, c * 0.62, ring * 0.5);
        c += float3(1.0) * smoothstep(0.42, 0.0, length(bq + float2(0.02, 0.018)) / 0.06) * 0.65;
    }
    return half4(half3(c * a), half(a));
}
"""
