#version 100

attribute vec2 pos;
attribute vec2 tpos;

varying vec2 vPos;

void main() {
    gl_Position = vec4(pos, 0.0, 1.0);
    vPos = tpos;
}
