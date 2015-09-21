package org.renjin.maven.namespace;

import com.google.common.collect.Lists;
import org.renjin.eval.Context;
import org.renjin.eval.EvalException;
import org.renjin.eval.SessionBuilder;
import org.renjin.packaging.LazyLoadFrameBuilder;
import org.renjin.parser.RParser;
import org.renjin.primitives.packaging.FqPackageName;
import org.renjin.primitives.packaging.Namespace;
import org.renjin.sexp.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NamespaceBuilder {

  private FqPackageName name;
  private File sourceDirectory;
  private File environmentFile;
  private List<String> defaultPackages;

  public void build(String groupId, String namespaceName, File sourceDirectory,
      File environmentFile, List<String> defaultPackages) throws IOException {

    this.name = new FqPackageName(groupId, namespaceName);
    this.sourceDirectory = sourceDirectory;
    this.environmentFile = environmentFile;
    this.defaultPackages = defaultPackages;

    compileNamespaceEnvironment();
  }


  private void compileNamespaceEnvironment()  {
    List<File> sources = getRSources();
    if(isUpToDate(sources)) {
      return;
    }
    
    Context context = initContext();

    Namespace namespace = context.getNamespaceRegistry().createNamespace(new InitializingPackage(name));
    evaluateSources(context, getRSources(), namespace.getNamespaceEnvironment());
    serializeEnvironment(context, namespace.getNamespaceEnvironment(), environmentFile);
  }

  private boolean isUpToDate(List<File> sources) {
    long lastModified = 0;
    for(File source : sources) {
      if(source.lastModified() > lastModified) {
        lastModified = source.lastModified();
      }
    }
    
    if(lastModified < environmentFile.lastModified()) {
      System.out.println("namespaceEnvironment is up to date, skipping compilation");
      return true;
    }
    
    return false;
  }

  private Context initContext()  {
    SessionBuilder builder = new SessionBuilder();
    Context context = builder.build().getTopLevelContext();
    if(defaultPackages != null) {
      for(String name : defaultPackages) {
        context.evaluate(FunctionCall.newCall(Symbol.get("library"), StringVector.valueOf(name)));
      }
    }
    return context;
  }

  private List<File> getRSources() {
    List<File> list = Lists.newArrayList();
    File[] files = sourceDirectory.listFiles();
    if(files != null) {
      list.addAll(Arrays.asList(files));
    }
    Collections.sort(list);
    return list;
  }


  private void evaluateSources(Context context, List<File> sources, Environment namespaceEnvironment)  {
    for(File sourceFile : sources) {
      String nameUpper = sourceFile.getName().toUpperCase();
      if(nameUpper.endsWith(".R") ||
         nameUpper.endsWith(".S") ||
         nameUpper.endsWith(".Q")) {
        System.err.println("Evaluating '" + sourceFile + "'");
        try {
          FileReader reader = new FileReader(sourceFile);
          SEXP expr = RParser.parseAllSource(reader);
          reader.close();

          context.evaluate(expr, namespaceEnvironment);
        
        } catch (EvalException e) {
          System.out.println("ERROR: " + e.getMessage());
          e.printRStackTrace(System.out);
          throw new RuntimeException("Error evaluating package source: " + sourceFile.getName(), e);
        } catch (Exception e) {
          throw new RuntimeException("Exception evaluating " + sourceFile.getName(), e);
        }
      }
    }
  }
  
  private void serializeEnvironment(Context context, Environment namespaceEnv, File environmentFile) {
    
    System.out.println("Writing namespace environment to " + environmentFile);
    try {
      LazyLoadFrameBuilder builder = new LazyLoadFrameBuilder(context);
      builder.outputTo(environmentFile.getParentFile());
      builder.build(namespaceEnv);
    } catch(IOException e) {
      throw new RuntimeException("Exception encountered serializing namespace environment", e);
    }
  }

}

