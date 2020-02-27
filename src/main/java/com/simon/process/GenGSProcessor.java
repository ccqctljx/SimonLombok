package com.simon.process;

import com.simon.annotation.SimonData;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.Set;

import static com.simon.process.GenerateGSTools.*;

/**
 * @Date 2020/2/27 20:08
 */
@SupportedAnnotationTypes("com.simon.annotation.SimonData")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class GenGSProcessor extends AbstractProcessor {


    /**
     * Messager接口提供注解处理器用来报告错误消息、警告和其他通知的方式
     * 它不是注解处理器开发者的日志工具，而是用来写一些信息给使用此注解器的第三方开发者的
     * 注意：我们应该对在处理过程中可能发生的异常进行捕获，通过Messager接口提供的方法通知用户（在官方文档中描述了消息的不同级别。非常重要的是Kind.ERROR）。
     * 此外，使用带有Element参数的方法连接到出错的元素，
     * 用户可以直接点击错误信息跳到出错源文件的相应行。
     * 如果你在process()中抛出一个异常，那么运行注解处理器的JVM将会崩溃（就像其他Java应用一样），
     * 这样用户会从javac中得到一个非常难懂出错信息
     */
    private Messager messager;

    /**
     * 实现Filer接口的对象，用于创建文件、类和辅助文件。
     * 使用Filer你可以创建文件
     * Filer中提供了一系列方法,可以用来创建class、java、resources文件
     * filer.createClassFile()[创建一个新的类文件，并返回一个对象以允许写入它]
     * filer.createResource() [创建一个新的源文件，并返回一个对象以允许写入它]
     * filer.createSourceFile() [创建一个用于写入操作的新辅助资源文件，并为它返回一个文件对象]
     */
    private Filer filer;

    /**
     * 用来处理Element的工具类
     * Elements接口的对象，用于操作元素的工具类。
     */
    private JavacElements elementUtils;

    /**
     * 用来处理TypeMirror的工具类
     * 实现Types接口的对象，用于操作类型的工具类。
     */
    private Types typeUtils;

    /**
     * 提供了待处理的抽象语法树
     * 这个依赖需要将${JAVA_HOME}/lib/tools.jar 添加到项目的classpath,IDE默认不加载这个依赖
     */
    private JavacTrees trees;

    /**
     * 这个依赖需要将${JAVA_HOME}/lib/tools.jar 添加到项目的classpath,IDE默认不加载这个依赖
     * TreeMaker 创建语法树节点的所有方法，创建时会为创建出来的JCTree设置pos字段，
     * 所以必须用上下文相关的TreeMaker对象来创建语法树节点，而不能直接new语法树节点。
     */
    private TreeMaker treeMaker;

    /**
     * 提供了创建标识符的方法
     */
    private Names names;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        System.out.println("GenGSProcessor Initial Start......");
        super.init(processingEnv);
        this.messager = processingEnv.getMessager();
        this.filer = processingEnv.getFiler();
        this.elementUtils = (JavacElements) processingEnv.getElementUtils();
        this.typeUtils = processingEnv.getTypeUtils();
        this.trees = JavacTrees.instance(processingEnv);
        Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
        this.treeMaker = TreeMaker.instance(context);
        this.names = Names.instance(context);
        System.out.println("GenGSProcessor Initial End......");
    }

    /**
     * 该方法将一轮一轮的遍历源代码
     * 处理注解前需要先获取两个重要信息，
     * 第一是注解本身的信息，具体来说就是获取注解对象，有了注解对象以后就可以获取注解的值。
     * 第二是被注解元素的信息，具体来说就是获取被注解的字段、方法、类等元素的信息
     *
     * @param annotations 该方法需要处理的注解类型
     * @param roundEnv    关于一轮遍历中提供给我们调用的信息.
     * @return 该轮注解是否处理完成 true 下轮或者其他的注解处理器将不会接收到次类型的注解.用处不大.
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        System.out.println("processing.......");

        // roundEnv.getRootElements()会返回工程中所有的Class,在实际应用中需要对各个Class先做过滤以提高效率，避免对每个Class的内容都进行扫描
        roundEnv.getRootElements();
        messager.printMessage(Diagnostic.Kind.NOTE, "SimonDataProcessor注解处理器处理中");
        TypeElement currentAnnotation = null;
        // 遍历注解集合,也即@SupportedAnnotationTypes中标注的类型
        for (TypeElement annotation : annotations) {
            messager.printMessage(Diagnostic.Kind.NOTE, "遍历本注解处理器处理的所有注解,当前遍历到的注解是：" + annotation.getSimpleName());
            currentAnnotation = annotation;
        }

        // 获取所有包含 SimonData 注解的元素 (roundEnv.getElementsAnnotatedWith(SimonData.class))
        // 返回所有被注解了 @SimonData 的元素的列表。
        Set<? extends Element> elementSet = roundEnv.getElementsAnnotatedWith(SimonData.class);
        messager.printMessage(Diagnostic.Kind.NOTE, "SimonDataProcessor注解处理器处理 @SimonData 注解");
        for (Element element : elementSet) {

            // 类名
            String className = element.getSimpleName().toString();
            messager.printMessage(Diagnostic.Kind.NOTE, "当前被标注注解的方法所在的类是：" + className);

            // 如果是类的话，获取子字段
            if (ElementKind.CLASS == element.getKind()) {
                // 这里根据类 Element 拿出抽象语法树
                JCTree tree = trees.getTree(element);
                tree.accept(new TreeTranslator(){
                    @Override
                    public void visitClassDef(JCTree.JCClassDecl jcClassDecl){
                        jcClassDecl.defs.stream()
                                // 拿出变量
                                .filter(k -> k.getKind().equals(Tree.Kind.VARIABLE))
                                .map(t -> (JCTree.JCVariableDecl) t)
                                .forEach(jcVariableDecl -> {
                                    //添加get方法
                                    jcClassDecl.defs = jcClassDecl.defs.append(makeGetterMethodDecl(jcVariableDecl));
                                    //添加set方法
                                    jcClassDecl.defs = jcClassDecl.defs.append(makeSetterMethodDecl(jcVariableDecl));
                                });
                        super.visitClassDef(jcClassDecl);
                    }
                });
            }
        }

        return true;
    }

    /**
     * 创建get方法
     *
     * @param jcVariableDecl
     * @return
     */
    private JCTree.JCMethodDecl makeGetterMethodDecl(JCTree.JCVariableDecl jcVariableDecl) {
        //方法的访问级别
        JCTree.JCModifiers modifiers = treeMaker.Modifiers(Flags.PUBLIC);
        //方法名称
        Name methodName = generateMethodNames(jcVariableDecl.getName(), GET_METHOD, names);
        //设置返回值类型
        JCTree.JCExpression returnMethodType = jcVariableDecl.vartype;
        // return 语句
        ListBuffer<JCTree.JCStatement> statements = new ListBuffer<>();
        statements.append(treeMaker.Return(treeMaker.Select(treeMaker.Ident(names.fromString("this")), jcVariableDecl.getName())));
        //设置方法体
        JCTree.JCBlock methodBody = treeMaker.Block(0, statements.toList());
        List<JCTree.JCTypeParameter> methodGenericParams = List.nil();
        List<JCTree.JCVariableDecl> parameters = List.nil();
        List<JCTree.JCExpression> throwsClauses = List.nil();
        //构建方法
        return treeMaker.MethodDef(modifiers, methodName, returnMethodType, methodGenericParams, parameters, throwsClauses, methodBody, null);
    }

    /**
     * 创建set方法
     *
     * @param jcVariableDecl
     * @return
     */
    private JCTree.JCMethodDecl makeSetterMethodDecl(JCTree.JCVariableDecl jcVariableDecl) {
        //方法的访问级别
        JCTree.JCModifiers modifiers = treeMaker.Modifiers(Flags.PUBLIC);
        //定义方法名
        Name methodName = generateMethodNames(jcVariableDecl.getName(), SET_METHOD, names);
        //定义返回值类型
        JCTree.JCExpression returnMethodType = treeMaker.TypeIdent(TypeTag.VOID);
        ListBuffer<JCTree.JCStatement> statements = new ListBuffer<>();

        //this.xxx = xxx;  setter方法中的赋值语句
        JCTree.JCStatement jcStatement = treeMaker.Exec(treeMaker.Assign(
                treeMaker.Select(treeMaker.Ident(names.fromString("this")), jcVariableDecl.getName()),
                treeMaker.Ident(jcVariableDecl.getName())));

        statements.append(jcStatement);

        //定义方法体
        JCTree.JCBlock methodBody = treeMaker.Block(0, statements.toList());
        List<JCTree.JCTypeParameter> methodGenericParams = List.nil();
        //定义入参
        JCTree.JCVariableDecl param = treeMaker.VarDef(treeMaker.Modifiers(Flags.PARAMETER, List.nil()), jcVariableDecl.name, jcVariableDecl.vartype, null);
        //设置入参
        List<JCTree.JCVariableDecl> parameters = List.of(param);
        List<JCTree.JCExpression> throwsClauses = List.nil();
        //构建新方法
        return treeMaker.MethodDef(modifiers, methodName, returnMethodType, methodGenericParams, parameters, throwsClauses, methodBody, null);

    }
    
}
