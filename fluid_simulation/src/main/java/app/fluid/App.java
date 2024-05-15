package app.fluid;

import processing.core.*;

public class App extends PApplet {
    Fluid fluid = new Fluid(0.1f, 0.0f, 0.0f);

    float t = 0;

    public void settings() {
        size(fluid.N * fluid.Scale, fluid.N * fluid.Scale);
    }

    public void draw() {
        background(0);
        int dx = ((width / 2) / fluid.Scale);
        int dy = ((height / 2) / fluid.Scale);
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                fluid.AddDensity(dx, dy, random(30, 100));
            }
        }

        float angle = noise(t) * TWO_PI;
        PVector v = PVector.fromAngle(angle);
        v.mult(0.5f);
        t += 0.005;
        fluid.AddVelocity(dx, dy, v.x, v.y);

        fluid.FluidStep();
        RenderD();
    }

    public static void main(String[] args) {
        PApplet.main("app.fluid.App");
    }

    void RenderD() {
        for (int i = 0; i < fluid.N; i++) {
            for (int j = 0; j < fluid.N; j++) {
                float x = i * fluid.Scale;
                float y = j * fluid.Scale;
                float d = fluid.density[fluid.IX(i, j)];
                fill(0,d, 0);
                noStroke();
                rect(x, y, fluid.Scale, fluid.Scale);
            }
        }
    }
}
