package sdd.validate;

public class SddValidateExtension {
    private String domain = "inventory";
    private String specsDir = "specs";
    private String stepsDir = "src/test/java";
    private boolean failOnDivergence = true;

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public String getSpecsDir() { return specsDir; }
    public void setSpecsDir(String specsDir) { this.specsDir = specsDir; }
    public String getStepsDir() { return stepsDir; }
    public void setStepsDir(String stepsDir) { this.stepsDir = stepsDir; }
    public boolean isFailOnDivergence() { return failOnDivergence; }
    public void setFailOnDivergence(boolean fail) { this.failOnDivergence = fail; }
}
