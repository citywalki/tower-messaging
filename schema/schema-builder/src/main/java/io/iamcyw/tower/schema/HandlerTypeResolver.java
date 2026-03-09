package io.iamcyw.tower.schema;

import io.iamcyw.tower.messaging.Command;
import io.iamcyw.tower.messaging.CommandHandler;
import io.iamcyw.tower.schema.model.HandlerTypeInfo;
import org.jboss.jandex.*;

import java.util.*;

/**
 * Resolves generic type parameters from CommandHandler implementations using Jandex.
 *
 * <p>This class extracts the command type (C) and result type (R) from classes that
 * implement {@code CommandHandler<C, R>}. It handles:
 * <ul>
 *   <li>Direct interface implementation: {@code class MyHandler implements CommandHandler<MyCmd, MyResult>}</li>
 *   <li>Inherited generics via superclass: {@code class MyHandler extends BaseHandler<MyCmd, MyResult>}</li>
 *   <li>Interface inheritance: {@code interface MyHandler extends CommandHandler<C, R>}</li>
 *   <li>Recursive interface checking through the class hierarchy</li>
 * </ul>
 *
 * <p>The resolver validates that the command type implements the {@link Command} marker interface,
 * ensuring type safety at build time.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * IndexView index = ...; // Jandex index
 * HandlerTypeResolver resolver = new HandlerTypeResolver();
 *
 * // Resolve a single handler
 * ClassInfo handlerClass = index.getClassByName(DotName.createSimple("com.example.MyHandler"));
 * HandlerTypeInfo info = resolver.resolve(handlerClass, index);
 *
 * // Resolve all handlers in the index
 * List&lt;HandlerTypeInfo&gt; allHandlers = resolver.resolveAll(index);
 * </pre>
 *
 * @see CommandHandler
 * @see HandlerTypeInfo
 * @since 2.0
 */
public class HandlerTypeResolver {

    private static final DotName COMMAND_HANDLER_NAME = DotName.createSimple(CommandHandler.class.getName());
    private static final DotName COMMAND_NAME = DotName.createSimple(Command.class.getName());

    /**
     * Resolves all CommandHandler implementations found in the given Jandex index.
     *
     * <p>This method scans the index for all known implementors of the {@link CommandHandler}
     * interface and extracts their generic type parameters.</p>
     *
     * @param index the Jandex index to scan, must not be null
     * @return a list of HandlerTypeInfo for all discovered handlers, never null
     * @throws NullPointerException if index is null
     * @throws SchemaBuilderException if a handler's generic types cannot be resolved
     */
    public List<HandlerTypeInfo> resolveAll(IndexView index) {
        Objects.requireNonNull(index, "IndexView must not be null");

        Collection<ClassInfo> handlers = index.getAllKnownImplementors(COMMAND_HANDLER_NAME);
        List<HandlerTypeInfo> result = new ArrayList<>();

        for (ClassInfo handlerClass : handlers) {
            // Skip abstract classes - only concrete implementations
            if (isAbstract(handlerClass)) {
                continue;
            }

            HandlerTypeInfo info = resolve(handlerClass, index);
            result.add(info);
        }

        return result;
    }

    /**
     * Resolves the generic type parameters for a single CommandHandler implementation.
     *
     * <p>This method extracts the C and R type parameters from the handler class's
     * implementation of {@code CommandHandler<C, R>}. It handles both direct interface
     * implementation and inherited generics through superclasses.</p>
     *
     * @param handlerClass the handler class to analyze, must not be null
     * @param index        the Jandex index for looking up class information, must not be null
     * @return the resolved HandlerTypeInfo containing command type, result type, and handler class name
     * @throws NullPointerException if handlerClass or index is null
     * @throws SchemaBuilderException if the generic types cannot be resolved or if the
     *                                command type does not implement Command
     */
    public HandlerTypeInfo resolve(ClassInfo handlerClass, IndexView index) {
        Objects.requireNonNull(handlerClass, "Handler class must not be null");
        Objects.requireNonNull(index, "IndexView must not be null");

        // Track visited classes to avoid infinite loops in circular hierarchies
        Set<DotName> visited = new HashSet<>();

        Type[] typeParams = extractTypeParameters(handlerClass, index, visited);

        if (typeParams == null || typeParams.length < 2) {
            throw new SchemaBuilderException(
                "Cannot resolve generic type parameters for handler class: " + handlerClass.name() +
                ". Ensure the class implements CommandHandler<C extends Command, R> with concrete types.");
        }

        Type commandType = typeParams[0];
        Type resultType = typeParams[1];

        // Resolve ClassInfo for command type
        ClassInfo commandClassInfo = resolveClassInfo(commandType, index);
        if (commandClassInfo == null) {
            throw new SchemaBuilderException(
                "Cannot resolve command type class info for: " + commandType +
                " in handler: " + handlerClass.name());
        }

        // Validate that command type implements Command interface
        if (!implementsCommand(commandClassInfo, index)) {
            throw new SchemaBuilderException(
                "Command type " + commandClassInfo.name() +
                " must implement " + Command.class.getName() +
                " in handler: " + handlerClass.name());
        }

        // Resolve ClassInfo for result type (may be null for void)
        ClassInfo resultClassInfo = resolveClassInfo(resultType, index);

        return new HandlerTypeInfo(commandClassInfo, resultClassInfo, handlerClass.name().toString());
    }

    /**
     * Extracts the C and R type parameters from the CommandHandler interface implementation.
     *
     * <p>This method recursively checks:
     * <ol>
     *   <li>Direct interface implementations on the current class</li>
     *   <li>Superclass (if the generic info is inherited)</li>
     *   <li>Interfaces of superclasses</li>
     *   <li>Interface inheritance chains</li>
     * </ol>
     *
     * @param classInfo the class to check
     * @param index     the Jandex index
     * @param visited   set of already visited class names to prevent infinite loops
     * @return array containing [commandType, resultType], or null if not found
     */
    private Type[] extractTypeParameters(ClassInfo classInfo, IndexView index, Set<DotName> visited) {
        if (classInfo == null) {
            return null;
        }

        // Prevent infinite loops in circular hierarchies
        if (!visited.add(classInfo.name())) {
            return null;
        }

        // Check direct interface implementations
        Type[] params = extractFromInterfaces(classInfo.interfaceTypes());
        if (params != null) {
            return params;
        }

        // Check interface inheritance recursively
        params = extractFromInterfaceInheritance(classInfo, index, visited);
        if (params != null) {
            return params;
        }

        // Check superclass recursively
        if (classInfo.superClassType() != null) {
            Type superClassType = classInfo.superClassType();

            // If superclass is parameterized, we need to resolve type variables
            if (superClassType.kind() == Type.Kind.PARAMETERIZED_TYPE) {
                ParameterizedType parameterizedSuper = superClassType.asParameterizedType();
                ClassInfo superClassInfo = index.getClassByName(parameterizedSuper.name());

                if (superClassInfo != null) {
                    // Recursively check superclass
                    Type[] superParams = extractTypeParameters(superClassInfo, index, visited);

                    // If superclass has the parameters, map them through the type arguments
                    if (superParams != null) {
                        return mapTypeVariables(superParams, parameterizedSuper.arguments(), superClassInfo);
                    }
                }
            } else {
                // Non-parameterized superclass - check it directly
                ClassInfo superClassInfo = index.getClassByName(superClassType.name());
                if (superClassInfo != null) {
                    Type[] superParams = extractTypeParameters(superClassInfo, index, visited);
                    if (superParams != null) {
                        return superParams;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Extracts type parameters from interface inheritance chains.
     *
     * <p>This handles cases where an interface extends CommandHandler:
     * {@code interface MyHandler extends CommandHandler<MyCmd, MyResult>}</p>
     *
     * @param classInfo the class whose interfaces to check
     * @param index     the Jandex index
     * @param visited   set of already visited class names
     * @return array containing [commandType, resultType] if found, null otherwise
     */
    private Type[] extractFromInterfaceInheritance(ClassInfo classInfo, IndexView index, Set<DotName> visited) {
        for (Type interfaceType : classInfo.interfaceTypes()) {
            ClassInfo interfaceInfo = index.getClassByName(interfaceType.name());
            if (interfaceInfo == null) {
                continue;
            }

            // Check if this interface directly implements CommandHandler
            Type[] params = extractFromInterfaces(interfaceInfo.interfaceTypes());
            if (params != null) {
                // If the interface type is parameterized, map type variables
                if (interfaceType.kind() == Type.Kind.PARAMETERIZED_TYPE) {
                    ParameterizedType parameterizedInterface = interfaceType.asParameterizedType();
                    return mapTypeVariables(params, parameterizedInterface.arguments(), interfaceInfo);
                }
                return params;
            }

            // Recursively check the interface's interfaces
            params = extractFromInterfaceInheritance(interfaceInfo, index, visited);
            if (params != null) {
                // Map through the interface's type arguments if parameterized
                if (interfaceType.kind() == Type.Kind.PARAMETERIZED_TYPE) {
                    ParameterizedType parameterizedInterface = interfaceType.asParameterizedType();
                    return mapTypeVariables(params, parameterizedInterface.arguments(), interfaceInfo);
                }
                return params;
            }
        }
        return null;
    }

    /**
     * Extracts type parameters from the list of interface types.
     *
     * @param interfaceTypes list of interface types implemented by a class
     * @return array containing [commandType, resultType] if CommandHandler is found, null otherwise
     */
    private Type[] extractFromInterfaces(List<Type> interfaceTypes) {
        for (Type interfaceType : interfaceTypes) {
            if (interfaceType.name().equals(COMMAND_HANDLER_NAME)) {
                if (interfaceType.kind() == Type.Kind.PARAMETERIZED_TYPE) {
                    List<Type> args = interfaceType.asParameterizedType().arguments();
                    if (args.size() >= 2) {
                        return new Type[]{args.get(0), args.get(1)};
                    }
                }
            }
        }
        return null;
    }

    /**
     * Maps type variables to their actual types based on the parameterized type arguments.
     *
     * <p>When a class extends a parameterized superclass like
     * {@code BaseHandler<C, R> implements CommandHandler<C, R>},
     * we need to map the type variables C and R to their actual types.</p>
     *
     * <p>This method uses name-based resolution to correctly handle cases where type variables
     * are in different positions or have different names in the hierarchy.</p>
     *
     * @param params        the type parameters from the superclass (may contain TypeVariable references)
     * @param arguments     the actual type arguments from the parameterized type
     * @param targetType    the target type (superclass or interface) that declares the type parameters
     * @return array with type variables replaced by actual types
     */
    private Type[] mapTypeVariables(Type[] params, List<Type> arguments, ClassInfo targetType) {
        // Build a name-to-type mapping from the target type's type parameters
        Map<String, Type> nameToTypeMap = new HashMap<>();

        List<TypeVariable> typeParameters = targetType.typeParameters();
        for (int i = 0; i < typeParameters.size() && i < arguments.size(); i++) {
            TypeVariable typeVar = typeParameters.get(i);
            nameToTypeMap.put(typeVar.identifier(), arguments.get(i));
        }

        // Map each parameter using name-based lookup
        Type[] mapped = new Type[params.length];
        for (int i = 0; i < params.length; i++) {
            mapped[i] = resolveTypeVariable(params[i], nameToTypeMap);
        }
        return mapped;
    }

    /**
     * Resolves a type variable to its actual type using the name-to-type mapping.
     *
     * <p>If the type is a TypeVariable, it looks up the actual type by name.
     * Otherwise, returns the type as-is.</p>
     *
     * @param type          the type to resolve
     * @param nameToTypeMap mapping from type variable name to actual type
     * @return the resolved type, or the original type if not a type variable
     */
    private Type resolveTypeVariable(Type type, Map<String, Type> nameToTypeMap) {
        if (type.kind() == Type.Kind.TYPE_VARIABLE) {
            TypeVariable typeVar = type.asTypeVariable();
            Type resolved = nameToTypeMap.get(typeVar.identifier());
            if (resolved != null) {
                return resolved;
            }
        } else if (type.kind() == Type.Kind.PARAMETERIZED_TYPE) {
            // Recursively resolve type variables in parameterized type arguments
            ParameterizedType pType = type.asParameterizedType();
            List<Type> args = pType.arguments();
            boolean hasTypeVar = false;
            for (Type arg : args) {
                if (arg.kind() == Type.Kind.TYPE_VARIABLE) {
                    hasTypeVar = true;
                    break;
                }
            }
            if (hasTypeVar) {
                // Need to create a new parameterized type with resolved arguments
                Type[] resolvedArgs = new Type[args.size()];
                for (int i = 0; i < args.size(); i++) {
                    resolvedArgs[i] = resolveTypeVariable(args.get(i), nameToTypeMap);
                }
                // Return a new parameterized type with resolved arguments
                return ParameterizedType.create(pType.name(), resolvedArgs, null);
            }
        }
        return type;
    }

    /**
     * Resolves a Type to its ClassInfo.
     *
     * @param type  the type to resolve
     * @param index the Jandex index
     * @return the ClassInfo for the type, or null if it cannot be resolved
     */
    private ClassInfo resolveClassInfo(Type type, IndexView index) {
        if (type == null) {
            return null;
        }

        DotName typeName;
        if (type.kind() == Type.Kind.PARAMETERIZED_TYPE) {
            typeName = type.asParameterizedType().name();
        } else if (type.kind() == Type.Kind.CLASS) {
            typeName = type.asClassType().name();
        } else if (type.kind() == Type.Kind.TYPE_VARIABLE) {
            // Type variable without resolution - cannot determine concrete type
            return null;
        } else {
            return null;
        }

        return index.getClassByName(typeName);
    }

    /**
     * Checks if a class implements the Command marker interface.
     *
     * <p>This method recursively checks the class and all its superclasses and interfaces
     * to determine if Command is implemented anywhere in the hierarchy.</p>
     *
     * @param classInfo the class to check
     * @param index     the Jandex index for looking up superclass information
     * @return true if the class implements Command
     */
    private boolean implementsCommand(ClassInfo classInfo, IndexView index) {
        return implementsCommand(classInfo, index, new HashSet<>());
    }

    /**
     * Recursive helper for implementsCommand with visited tracking.
     */
    private boolean implementsCommand(ClassInfo classInfo, IndexView index, Set<DotName> visited) {
        if (classInfo == null) {
            return false;
        }

        // Prevent infinite loops
        if (!visited.add(classInfo.name())) {
            return false;
        }

        // Check if it directly implements Command
        if (classInfo.interfaceNames().contains(COMMAND_NAME)) {
            return true;
        }

        // Check all interfaces recursively
        for (DotName interfaceName : classInfo.interfaceNames()) {
            ClassInfo interfaceInfo = index.getClassByName(interfaceName);
            if (interfaceInfo != null && implementsCommand(interfaceInfo, index, visited)) {
                return true;
            }
        }

        // Check superclass recursively
        if (classInfo.superName() != null) {
            ClassInfo superClassInfo = index.getClassByName(classInfo.superName());
            if (superClassInfo != null && implementsCommand(superClassInfo, index, visited)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if a class is abstract.
     *
     * @param classInfo the class to check
     * @return true if the class is abstract
     */
    private boolean isAbstract(ClassInfo classInfo) {
        return (classInfo.flags() & 0x00000400) != 0; // ACC_ABSTRACT
    }

}
