package io.github.egd.prodigal.dynamic.sample;

public class TestPO {

    private int id;

    private String name;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "TestPO [id=" + id + ", name=" + name + "]";
    }

}
