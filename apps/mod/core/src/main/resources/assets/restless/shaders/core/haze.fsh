#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:oit.glsl>

uniform sampler2D Sampler0;

layout(location = 0) in float sphericalVertexDistance;
layout(location = 1) in float cylindricalVertexDistance;
layout(location = 2) in vec2 texCoord0;
layout(location = 3) in vec4 vertexColor;

#ifndef OIT_ALPHA_ONLY
layout(location = 0) out vec4 fragColor;
#endif

// Volumetric-looking haze drawn on a camera-facing quad. The quad's texture coordinates run 0..1 across it (the
// particle sets them). The shader treats the quad as a thick soft cloud: a smooth density (a gaussian body broken up by
// domain-warped noise, with no hard thresholds, so there is no contour anywhere), lit by a fixed light so the side of
// the cloud facing it is lighter and the far side darker. It fades out near the camera so walking into the haze never
// shows the quad's plane.

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float valueNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x), mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
}

float fbm(vec2 p) {
    float sum = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 3; i++) {
        sum += amplitude * valueNoise(p);
        p = p * 2.03 + 17.1;
        amplitude *= 0.5;
    }
    return sum / 0.875;
}

// Cloud density at a point of the quad, 0..~1.3: round soft body times warped noise.
float density(vec2 p, vec2 warp) {
    float r2 = dot(p, p);
    float soft = 1.0 - smoothstep(0.0, 0.8, sqrt(r2));
    float body = soft * soft;
    float cloud = fbm(p * 1.7 + warp * 1.6);
    return body * mix(0.55, 1.15, cloud);
}

vec4 calculateFinalColor(vec4 color) {
    #ifdef OIT_ACCUMULATE
    color = sampleColorForAccumulation(color);
    vec4 fogColor = vec4(FogColor.rgb * color.a, FogColor.a);
    #else
    vec4 fogColor = FogColor;
    #endif
    return apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, fogColor);
}

void main() {
    vec2 p = texCoord0 * 2.0 - 1.0;
    float r2 = dot(p, p);
    if (r2 >= 1.0) {
        discard;
    }
    vec2 warp = vec2(fbm(p * 1.3 + 3.1), fbm(p * 1.3 - 7.7)) - 0.5;
    float here = density(p, warp);
    // The cloud is thicker where more of it lies toward the light, so the lit side reads as the cloud's surface.
    float toward = density(p + vec2(0.22, 0.3), warp);
    float shade = clamp(0.5 + (here - toward) * 0.8, 0.0, 1.0);
    float nearFade = smoothstep(1.0, 4.5, sphericalVertexDistance);
    // The sampler stays bound by the shared particle layout, so it has to be read once; the weight keeps it from showing.
    float keep = 1.0 + 0.00001 * texture(Sampler0, texCoord0).a;
    vec3 rgb = vertexColor.rgb * mix(0.85, 1.15, shade);
    float alpha = vertexColor.a * clamp(here * 0.9, 0.0, 1.0) * nearFade * keep;
    vec4 color = vec4(rgb, alpha) * ColorModulator;
    if (color.a < 0.002) {
        discard;
    }
    #ifdef OIT_ALPHA_ONLY
    executeAlphaOnlyPhase(gl_FragCoord.z, color.a);
    #else
    fragColor = calculateFinalColor(color);
    #endif
}
