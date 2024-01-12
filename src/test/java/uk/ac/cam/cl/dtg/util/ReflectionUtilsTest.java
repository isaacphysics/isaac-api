package uk.ac.cam.cl.dtg.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static uk.ac.cam.cl.dtg.util.ReflectionUtils.getClasses;
import static uk.ac.cam.cl.dtg.util.ReflectionUtils.getSubTypes;

import java.util.Set;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.GameboardDO;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.Formula;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;
import uk.ac.cam.cl.dtg.isaac.dos.users.AbstractSegueUser;
import uk.ac.cam.cl.dtg.isaac.dos.users.AnonymousUser;
import uk.ac.cam.cl.dtg.isaac.dos.users.RegisteredUser;

class ReflectionUtilsTest {
  @Test
  void getClassesShouldReturnClassesInPackage() {
    String targetPackage = "uk.ac.cam.cl.dtg.isaac.api";
    Set<Class<?>> classes = getClasses(targetPackage);
    assertFalse(classes.isEmpty());
    assertEquals(classes.size(), classes.stream().filter(c -> c.getPackageName().startsWith(targetPackage)).count());
  }

  @Test
  void getSubTypesShouldFilterClasses() {
    Set<Class<? extends Content>> expectedFilteredClasses = Set.of(Question.class, Formula.class);
    Set<Class<?>> classes = Set.of(GameboardDO.class, Question.class, Formula.class);
    Set<Class<? extends Content>> filteredClasses = getSubTypes(classes, Content.class);
    assertEquals(expectedFilteredClasses, filteredClasses);
  }

  @Test
  void getSubTypesAlsoWorksForTargetsOtherThanContent() {
    Set<Class<? extends AbstractSegueUser>> expectedFilteredClasses = Set.of(AnonymousUser.class, RegisteredUser.class);
    Set<Class<?>> classes = Set.of(Question.class, Formula.class, AnonymousUser.class, RegisteredUser.class);
    Set<Class<? extends AbstractSegueUser>> filteredClasses = getSubTypes(classes, AbstractSegueUser.class);
    assertEquals(expectedFilteredClasses, filteredClasses);
  }
}