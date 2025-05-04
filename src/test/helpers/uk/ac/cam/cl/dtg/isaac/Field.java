package uk.ac.cam.cl.dtg.isaac;

public class Field {
  private String name;
  private int marks;

  public static Field field() {
      return new Field();
  }

  public Field setName(String jsonField) {
      this.name = jsonField;
      return this;
  }

  public String name() {
      return name;
  }

  public Field setMarks(int marks) {
      this.marks = marks;
      return this;
  }

  public int marks() {
      return marks;
  }
}