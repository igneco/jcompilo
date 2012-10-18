package com.googlecode.compilo.tco;

import com.googlecode.compilo.AsmMethodHandler;
import com.googlecode.compilo.Resource;
import com.googlecode.compilo.ResourceHandler;
import com.googlecode.totallylazy.Pair;
import com.googlecode.totallylazy.Sequence;
import com.googlecode.totallylazy.collections.ImmutableList;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.CheckClassAdapter;

import java.lang.annotation.Annotation;

import static com.googlecode.compilo.Resource.constructors.resource;
import static com.googlecode.compilo.tco.Asm.annotations;
import static com.googlecode.compilo.tco.Asm.hasAnnotation;
import static com.googlecode.compilo.tco.Asm.predicates.annotation;
import static com.googlecode.totallylazy.Debug.debugging;
import static com.googlecode.totallylazy.collections.ImmutableList.constructors;
import static com.googlecode.totallylazy.collections.ImmutableList.constructors.list;

public class AsmResourceHandler implements ResourceHandler {
    private final ImmutableList<Pair<Class<? extends Annotation>, AsmMethodHandler>> processors;
    private final boolean verify;

    private AsmResourceHandler(Iterable<? extends Pair<Class<? extends Annotation>, AsmMethodHandler>> processors, boolean verify) {
        this.processors = list(processors);
        this.verify = verify;
    }

    public static AsmResourceHandler asmResourceHandler(Iterable<? extends Pair<Class<? extends Annotation>, AsmMethodHandler>> processors, boolean verify) {
        return new AsmResourceHandler(processors, verify);
    }

    public static AsmResourceHandler asmResourceHandler(Iterable<? extends Pair<Class<? extends Annotation>, AsmMethodHandler>> processors) {
        return asmResourceHandler(processors, debugging());
    }

    public static AsmResourceHandler asmResourceHandler(boolean verify) {
        return asmResourceHandler(constructors.<Pair<Class<? extends Annotation>, AsmMethodHandler>>empty(), verify);
    }

    public static AsmResourceHandler asmResourceHandler() {
        return asmResourceHandler(debugging());
    }

    public AsmResourceHandler add(Class<? extends Annotation> annotation, AsmMethodHandler asmProcessor) {
        return asmResourceHandler(processors.cons(Pair.<Class<? extends Annotation>, AsmMethodHandler>pair(annotation, asmProcessor)), verify);
    }

    @Override
    public boolean matches(String name) {
        return name.endsWith(".class");
    }

    @Override
    public Resource handle(Resource resource) {
        ClassReader reader = new ClassReader(resource.bytes());
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, 0);

        Sequence<MethodNode> methods = Asm.<MethodNode>seq(classNode.methods);
        for (MethodNode method : methods) {
            for (Pair<Class<? extends Annotation>, AsmMethodHandler> p : processors) {
                if (hasAnnotation(method, p.first())) {
                    p.second().process(classNode, method);
                    method.invisibleAnnotations.remove(annotations(method).find(annotation(p.first())).get());
                }
            }
        }

        ClassWriter writer = new ClassWriter(0);
        classNode.accept(verify ? new CheckClassAdapter(writer) : writer);
        return resource(resource.name(), writer.toByteArray());
    }
}
