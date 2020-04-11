package com.docwei.processor;

import com.docwei.annotation.Subscriber;
import com.docwei.annotation.ThreadMode;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/*javaCompileOptions {
        annotationProcessorOptions {
        arguments = [eventBusIndex: 'com.docwei.eventbusnewdemo.MainEventBusIndex']
        }
        }*/


@SupportedAnnotationTypes("com.docwei.annotation.Subscriber")
@SupportedOptions(value = {"eventBusIndex"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class EventBusAnnotationProcessor extends AbstractProcessor {
    protected ProcessingEnvironment processingEnv;
    private Elements mElments;
    private Messager mMessager;
    private Filer mFiler;
    private Map<String, String> mOptions;
    private Types mTypes;
    private String mIndex;
    private Map<String, List<SubscriberMethodInfo>> methodByclass = new HashMap<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mElments = processingEnv.getElementUtils();
        mMessager = processingEnv.getMessager();
        mFiler = processingEnv.getFiler();
        mOptions = processingEnv.getOptions();
        mTypes = processingEnv.getTypeUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        //判断有没有设置options
        // arguments = [eventBusIndex: 'com.docwei.eventbusnewdemo.MainEventBusIndex']
        //这个就是文件名
        mIndex = mOptions.get("eventBusIndex");
        if (mIndex == null) {
            mMessager.printMessage(Diagnostic.Kind.ERROR, "you must set filePath");
            return true;
        }
        //先获取所有含有Subscriber注解的字段：注意Subscriber注解没加@Inherited 不支持继承，简单一点
        //所有的Subscriber注解都只能注解到方法上，所以可以强转
        Set<ExecutableElement> set = (Set<ExecutableElement>) roundEnv.getElementsAnnotatedWith(Subscriber.class);
        for (ExecutableElement executableElement : set) {
            //校验方法 是否是Public 修饰符
            Set<Modifier> modifiers = executableElement.getModifiers();
            if (modifiers.contains(Modifier.STATIC)) {
                mMessager.printMessage(Diagnostic.Kind.ERROR, "subscriber method  cannot use static Modifier");
                return true;
            }
            if (!modifiers.contains(Modifier.PUBLIC)) {
                mMessager.printMessage(Diagnostic.Kind.ERROR, "subscriber method  is not public ");
                return true;
            }

            Subscriber subscriber = executableElement.getAnnotation(Subscriber.class);
            if (subscriber == null) {
                break;
            }
            List<VariableElement> parameterElements = (List<VariableElement>) executableElement.getParameters();
            if (parameterElements.size() != 1) {
                mMessager.printMessage(Diagnostic.Kind.ERROR, "parameter is error ");
                return true;
            }
            //找参数 + 注解值
            VariableElement variableElement = parameterElements.get(0);
            //要考虑是泛型的情形
            TypeMirror typeMirror = variableElement.asType();
            // Check for generic type 泛型类（TypeVariable）
            if (typeMirror instanceof TypeVariable) {
                //获取泛型的上界
                TypeMirror upperBound = ((TypeVariable) typeMirror).getUpperBound();
                //获取具体类型
                if (upperBound instanceof DeclaredType) {
                    typeMirror = upperBound;
                }
            }
            if (!(typeMirror instanceof DeclaredType) || !(((DeclaredType) typeMirror).asElement() instanceof TypeElement)) {
                mMessager.printMessage(Diagnostic.Kind.ERROR, "variableElement is error ");
                return true;
            }
            //参数的值
            String eventType = ((TypeElement) ((DeclaredType) typeMirror).asElement()).getQualifiedName().toString() + ".class";
            ThreadMode threadMode = subscriber.threadMode();

            //找这个方法所在的类
            TypeElement typeElement = (TypeElement) executableElement.getEnclosingElement();
            String clazzName = typeElement.getQualifiedName().toString() + ".class";
            List<SubscriberMethodInfo> list = methodByclass.get(clazzName);
            if (list == null) {
                list = new ArrayList<>();
            }
            list.add(new SubscriberMethodInfo(eventType, "ThreadMode." + threadMode, executableElement.getSimpleName().toString()));
            methodByclass.put(clazzName, list);

            //都找齐了，开始生成文件
            writeKeyLines();

        }
        createFile();
        return true;
    }

    private String writeKeyLines() {
        /*SUBSCRIBER_INDEX.put(xxx.class,new SubscribeMethodApt[]{
                new SubscribeMethodApt("xxx",xxx.class,com.docwei.annotation.ThreadMode.ASYN),
        }

        );*/
        StringBuilder builder = new StringBuilder();

        for (Map.Entry<String, List<SubscriberMethodInfo>> entry : methodByclass.entrySet()) {
            String subscriberClazz = entry.getKey();
            List<SubscriberMethodInfo> list = entry.getValue();
            builder.append("         SUBSCRIBER_INDEX.put(").append(subscriberClazz).append(",")
                    .append("new SubscribeMethodApt[]{ \n");
            int size = list.size();
            for (int i = 0; i < size; i++) {
                SubscriberMethodInfo methodInfo = list.get(i);
                if (i == 0) {
                    builder.append("        new SubscribeMethodApt(\"");
                } else {
                    builder.append("new SubscribeMethodApt(\"");
                }
                builder.append(methodInfo.method + "\"" + ",").append(methodInfo.eventType).append(",");

                if (i != size - 1) {
                    builder.append(methodInfo.threadmode + ")\n");
                    builder.append("        ,");
                } else {
                    builder.append(methodInfo.threadmode + ")}");
                }
            }
            builder.append(");\n");
        }
        return builder.toString();
    }


    //eventBus使用的是BufferedWriter
    private void createFile() {
        BufferedWriter bufferedWriter = null;
        try {
            JavaFileObject source = mFiler.createSourceFile(mIndex);
            bufferedWriter = new BufferedWriter(source.openWriter());
            int index = mIndex.lastIndexOf(".");
            String packageName = mIndex.substring(0, index);
            bufferedWriter.write("package " + packageName + ";\n\n");
            bufferedWriter.write("import java.util.HashMap;\n");
            bufferedWriter.write("import java.util.Map;\n");
            bufferedWriter.write("import " + ThreadMode.class.getCanonicalName() + ";\n");
            bufferedWriter.write("public class " + mIndex.substring(index + 1) + " implements ISubscriberMethodIndex{\n\n");
            bufferedWriter.write("     private final static Map<Class<?>, SubscribeMethodApt[]> SUBSCRIBER_INDEX;\n");
            bufferedWriter.write("     static {" + "\n");
            bufferedWriter.write("          SUBSCRIBER_INDEX = new HashMap<>();\n");

            bufferedWriter.write(writeKeyLines());

            bufferedWriter.write("     }" + "\n\n");
            bufferedWriter.write("     @Override" + "\n");
            bufferedWriter.write("     public SubscribeMethodApt[] getSubScriberMethod(Class<?> clazz){\n");
            bufferedWriter.write("        return SUBSCRIBER_INDEX.get(clazz);\n");
            bufferedWriter.write("      }\n");
            bufferedWriter.write("}\n");


        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bufferedWriter != null) {
                try {
                    bufferedWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

}
