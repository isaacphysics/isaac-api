package uk.ac.cam.cl.dtg.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReflectionUtils {
  private static final Logger log = LoggerFactory.getLogger(ReflectionUtils.class);

  private ReflectionUtils() {}

  public static Set<Class<?>> getClasses(String packageName) {
    try {
      List<File> dirs = findDirectories(packageName);
      Set<Class<?>> classes = new HashSet<>();
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      for (File directory : dirs) {
        classes.addAll(findClasses(directory, packageName, loader));
      }
      return classes;
    } catch (ClassNotFoundException e) {
      log.error("Error while retrieving class resources");
      throw new ClassReflectionException(e);
    } catch (IOException e) {
      log.error("IO error while retrieving resources");
      throw new ClassReflectionException(e);
    } catch (URISyntaxException e) {
      log.error("Error while parsing resource URI");
      throw new ClassReflectionException(e);
    }
  }

  private static List<File> findDirectories(String packageName) throws IOException, URISyntaxException {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    String path = packageName.replace('.', '/');
    Enumeration<URL> resources = classLoader.getResources(path);
    List<File> dirs = new ArrayList<>();
    while (resources.hasMoreElements()) {
      URL resource = resources.nextElement();
      URI uri = new URI(resource.toString());
      dirs.add(new File(uri.getPath()));
    }
    return dirs;
  }

  private static List<Class<?>> findClasses(File directory, String packageName, ClassLoader loader)
      throws ClassNotFoundException, IOException {
    List<Class<?>> classes = new ArrayList<>();
    if (!directory.exists()) {
      return classes;
    }
    File[] files = directory.listFiles();
    if (files == null) {
      return classes;
    }
    for (File file : files) {
      if (file.isDirectory()) {
        classes.addAll(findClasses(file, packageName + "." + file.getName(), loader));
      } else if (file.getName().endsWith(".class")) {
        classes.add(
            Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6), false, loader));
      }
    }
    return classes;
  }

  @SuppressWarnings("unchecked")
  public static <T> Set<Class<? extends T>> getSubTypes(Set<Class<?>> classes, Class<T> parentClass) {
    return classes.stream().filter(parentClass::isAssignableFrom).map(c -> (Class<T>) c).collect(Collectors.toSet());
  }
}
