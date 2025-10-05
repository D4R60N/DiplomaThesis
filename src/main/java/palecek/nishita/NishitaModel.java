package palecek.nishita;

import org.joml.Vector3f;

public class NishitaModel {

    private Vector3f sunDir;
    private Vector3f betaR;
    private Vector3f betaM;
    private float HR;
    private float HM;
    private float g;

    public NishitaModel(Vector3f sunDir, Vector3f betaR, Vector3f betaM, float HR, float HM, float g) {
        this.sunDir = sunDir;
        this.betaR = betaR;
        this.betaM = betaM;
        this.HR = HR;
        this.HM = HM;
        this.g = g;
    }

    public Vector3f getSunDir() {
        return sunDir;
    }
    public Vector3f getBetaR() {
        return betaR;
    }
    public Vector3f getBetaM() {
        return betaM;
    }
    public float getHR() {
        return HR;
    }
    public float getHM() {
        return HM;
    }
    public float getG() {
        return g;
    }
}
