#version 100
#extension GL_OES_EGL_image_external : require

precision mediump float;

varying vec2 vPos;
uniform samplerExternalOES hbSampler;

void main() {
    gl_FragColor = texture2D(hbSampler, vPos);
}





