package app.fluid;

import processing.core.*;

public class Fluid extends PApplet {
    final int N = 128;
    final int iter = 4;
    final int Scale = 5;

    int size;
    float dt;
    float diff;
    float visc;

    float[] s;
    float[] density;

    float[] Vx;
    float[] Vy;

    float[] Vx0;
    float[] Vy0;

    Fluid(float dt, float diffusion, float viscosity) {
        this.size = N;
        this.dt = dt;
        this.diff = diffusion;
        this.visc = viscosity;

        this.s = new float[size * size];
        this.density = new float[size * size];

        this.Vx = new float[size * size];
        this.Vy = new float[size * size];

        this.Vx0 = new float[size * size];
        this.Vy0 = new float[size * size];
    }

    int IX(int x, int y) {
        x = constrain(x, 0, N - 1);
        y = constrain(y, 0, N - 1);
        return x + y * size;
    }

    void AddDensity(int x, int y, float amount) {
        this.density[IX(x, y)] += amount;
    }

    void AddVelocity(int x, int y, float amountX, float amountY) {
        int index = IX(x, y);

        this.Vx[index] += amountX;
        this.Vy[index] += amountY;
    }

    void lin_solve(int b, float[] x, float[] x0, float a, float c) {
        float cRecip = 1 / c;
        for (int k = 0; k < iter; k++) {
            for (int j = 1; j < N - 1; j++) {
                for (int i = 1; i < N - 1; i++) {
                    x[IX(i, j)] = (x0[IX(i, j)]
                            + a * (x[IX(i + 1, j)]
                                    + x[IX(i - 1, j)]
                                    + x[IX(i, j + 1)]
                                    + x[IX(i, j - 1)]))
                            * cRecip;
                }
            }
            set_bnd(b, x);
        }
    }

    void set_bnd(int b, float[] x) {
        for (int i = 1; i < N - 1; i++) {
            x[IX(i, 0)] = b == 2 ? -x[IX(i, 1)] : x[IX(i, 1)];
            x[IX(i, N - 1)] = b == 2 ? -x[IX(i, N - 2)] : x[IX(i, N - 2)];
        }
        for (int j = 1; j < N - 1; j++) {
            x[IX(0, j)] = b == 1 ? -x[IX(1, j)] : x[IX(1, j)];
            x[IX(N - 1, j)] = b == 1 ? -x[IX(N - 2, j)] : x[IX(N - 2, j)];
        }

        x[IX(0, 0)] = 0.5f * (x[IX(1, 0)] + x[IX(0, 1)]);
        x[IX(0, N - 1)] = 0.5f * (x[IX(1, N - 1)] + x[IX(0, N - 2)]);
        x[IX(N - 1, 0)] = 0.5f * (x[IX(N - 2, 0)] + x[IX(N - 1, 1)]);
        x[IX(N - 1, N - 1)] = 0.5f * (x[IX(N - 2, N - 1)] + x[IX(N - 1, N - 2)]);
    }

    void diffuse(int b, float[] x, float[] x0, float diff, float dt) {
        float a = dt * diff * (N - 2) * (N - 2);
        lin_solve(b, x, x0, a, 1 + 6 * a);
    }

    void project(float[] velocX, float[] velocY, float[] p, float[] div) {
        for (int j = 1; j < N - 1; j++) {
            for (int i = 1; i < N - 1; i++) {
                div[IX(i, j)] = -0.5f * (velocX[IX(i + 1, j)]
                        - velocX[IX(i - 1, j)]
                        + velocY[IX(i, j + 1)]
                        - velocY[IX(i, j - 1)]) / N;
                p[IX(i, j)] = 0;
            }
        }
        set_bnd(0, div);
        set_bnd(0, p);
        lin_solve(0, p, div, 1, 6);

        for (int j = 1; j < N - 1; j++) {
            for (int i = 1; i < N - 1; i++) {
                velocX[IX(i, j)] -= 0.5f * (p[IX(i + 1, j)]
                        - p[IX(i - 1, j)]) * N;
                velocY[IX(i, j)] -= 0.5f * (p[IX(i, j + 1)]
                        - p[IX(i, j - 1)]) * N;
            }
        }
        set_bnd(1, velocX);
        set_bnd(2, velocY);
    }

    void advect(int b, float[] d, float[] d0, float[] velocX, float[] velocY, float dt) {
        float i0, i1, j0, j1;

        float dtx = dt * (N - 2);
        float dty = dt * (N - 2);

        float s0, s1, t0, t1;
        float tmp1, tmp2, x, y;

        float Nfloat = N;
        float ifloat, jfloat;
        int i, j;

        for (j = 1, jfloat = 1; j < N - 1; j++, jfloat++) {
            for (i = 1, ifloat = 1; i < N - 1; i++, ifloat++) {
                tmp1 = dtx * velocX[IX(i, j)];
                tmp2 = dty * velocY[IX(i, j)];
                x = ifloat - tmp1;
                y = jfloat - tmp2;

                if (x < 0.5f) x = 0.5f;
                if (x > Nfloat + 0.5f) x = Nfloat + 0.5f;
                i0 = floor(x);
                i1 = i0 + 1;

                if (y < 0.5f) y = 0.5f;
                if (y > Nfloat + 0.5f) y = Nfloat + 0.5f;
                j0 = floor(y);
                j1 = j0 + 1;

                s1 = x - i0;
                s0 = 1 - s1;
                t1 = y - j0;
                t0 = 1 - t1;

                int i0i = Math.round(i0);
                int i1i = Math.round(i1);
                int j0i = Math.round(j0);
                int j1i = Math.round(j1);

                d[IX(i, j)] =
                    s0 * (t0 * d0[IX(i0i, j0i)] + t1 * d0[IX(i0i, j1i)]) + 
                    s1 * (t0 * d0[IX(i1i, j0i)] + t1  * d0[IX(i1i, j1i)]);
            }
        }
        set_bnd(b, d);
    }

    void FluidStep() {

        float visc     = this.visc;
        float diff     = this.diff;
        float dt       = this.dt;
        float[] Vx      = this.Vx;
        float[] Vy      = this.Vy;
        float[] Vx0     = this.Vx0;
        float[] Vy0     = this.Vy0;
        float[] s       = this.s;
        float[] density = this.density;

        diffuse(1, Vx0, Vx, visc, dt);
        diffuse(2, Vy0, Vy, visc, dt);

        project(Vx0, Vy0, Vx, Vy);

        advect(1, Vx, Vx0, Vx0, Vy0, dt);
        advect(2, Vy, Vy0, Vx0, Vy0, dt);

        project(Vx, Vy, Vx0, Vy0);

        diffuse(0, s, density, diff, dt);
        advect(0, density, s, Vx, Vy, dt);
    }

    void fade(){
        for (int i = 0; i < N; i++) {
            float d = density[i];
            density[i] = constrain(d - 0.5f, 0, 255);
        }
    }
}
