/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.annotation.AnnotationTypeMapping.MirrorSets.MirrorSet;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Provides mapping information for a single annotation (or meta-annotation) in
 * the context of a root annotation type.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 5.2
 * @see AnnotationTypeMappings
 */
// 该类的主要作用是描述和管理单个注解与其根注解（Root Annotation）之间的映射关系。
// 在 Spring 中，一个注解可能是直接标注在类上的（Root），也可能是作为其他注解的元注解（Meta-annotation）存在的。AnnotationTypeMapping 负责处理以下复杂逻辑：
// 属性别名（@AliasFor）：处理同一注解内或跨注解层级的属性别名同步。
// 属性覆盖（Overrides）：建立元注解属性被上层注解覆盖的路径。
// 约定映射：处理早于 Spring 6.2 广泛使用的“基于名称一致”的隐式属性覆盖。
// 合成准备：计算注解是否需要被“合成”（Synthesized），即通过代理对象来统一读取别名后的值。
final class AnnotationTypeMapping {

	private static final Log logger = LogFactory.getLog(AnnotationTypeMapping.class);

	private static final Predicate<? super Annotation> isBeanValidationConstraint = annotation ->
			annotation.annotationType().getName().equals("jakarta.validation.Constraint");

	/**
	 * Set used to track which convention-based annotation attribute overrides
	 * have already been checked. Each key is the combination of the fully
	 * qualified class name of a composed annotation and a meta-annotation
	 * that it is either present or meta-present on the composed annotation,
	 * separated by a dash.
	 * @since 6.0
	 * @see #addConventionMappings()
	 */
	private static final Set<String> conventionBasedOverrideCheckCache = ConcurrentHashMap.newKeySet();

	private static final MirrorSet[] EMPTY_MIRROR_SETS = new MirrorSet[0];

	// 父级映射。如果当前是元注解，则指向包含它的那个注解映射。
	@Nullable
	private final AnnotationTypeMapping source;
	// 整个注解树的根节点映射。
	private final AnnotationTypeMapping root;
	// 距离根注解的深度。根注解为 0，直接元注解为 1，依此类推。
	private final int distance;
	// 当前映射对应的注解类型。
	private final Class<? extends Annotation> annotationType;
	// 从根到当前类型的完整路径列表。
	private final List<Class<? extends Annotation>> metaTypes;
	// 当前注解的实例对象（如果存在）。
	@Nullable
	private final Annotation annotation;
	// 当前注解类型的所有属性方法包装对象。
	private final AttributeMethods attributes;
	// 内部类，管理同一注解内互为别名的属性组。
	private final MirrorSets mirrorSets;
	// 核心映射表：索引为当前属性，值为对应根注解属性的索引（处理显式别名）。
	private final int[] aliasMappings;
	// 约定映射表：处理基于同名的隐式覆盖映射。
	private final int[] conventionMappings;
	// 最终值映射表：记录属性值应该从哪个索引处获取。
	private final int[] annotationValueMappings;
	// 记录每个属性值的来源映射对象。
	private final AnnotationTypeMapping[] annotationValueSource;
	// 倒排索引：记录哪个目标方法被当前注解的哪些方法起别名了。
	private final Map<Method, List<Method>> aliasedBy;
	// 标识该注解是否包含复杂的别名或覆盖，需要动态代理合成。
	private final boolean synthesizable;
	// 用于验证所有 @AliasFor 是否都已正确链接。
	private final Set<Method> claimedAliases = new HashSet<>();


	AnnotationTypeMapping(@Nullable AnnotationTypeMapping source, Class<? extends Annotation> annotationType,
			@Nullable Annotation annotation, Set<Class<? extends Annotation>> visitedAnnotationTypes) {

		this.source = source;
		this.root = (source != null ? source.getRoot() : this);
		this.distance = (source == null ? 0 : source.getDistance() + 1);
		this.annotationType = annotationType;
		this.metaTypes = merge(
				source != null ? source.getMetaTypes() : null,
				annotationType);
		this.annotation = annotation;
		this.attributes = AttributeMethods.forAnnotationType(annotationType);
		this.mirrorSets = new MirrorSets();
		this.aliasMappings = filledIntArray(this.attributes.size());
		this.conventionMappings = filledIntArray(this.attributes.size());
		this.annotationValueMappings = filledIntArray(this.attributes.size());
		this.annotationValueSource = new AnnotationTypeMapping[this.attributes.size()];
		this.aliasedBy = resolveAliasedForTargets();
		processAliases();
		addConventionMappings();
		addConventionAnnotationValues();
		this.synthesizable = computeSynthesizableFlag(visitedAnnotationTypes);
	}


	private static <T> List<T> merge(@Nullable List<T> existing, T element) {
		if (existing == null) {
			return Collections.singletonList(element);
		}
		List<T> merged = new ArrayList<>(existing.size() + 1);
		merged.addAll(existing);
		merged.add(element);
		return Collections.unmodifiableList(merged);
	}

	private Map<Method, List<Method>> resolveAliasedForTargets() {
		Map<Method, List<Method>> aliasedBy = new HashMap<>();
		for (int i = 0; i < this.attributes.size(); i++) {
			Method attribute = this.attributes.get(i);
			AliasFor aliasFor = AnnotationsScanner.getDeclaredAnnotation(attribute, AliasFor.class);
			if (aliasFor != null) {
				Method target = resolveAliasTarget(attribute, aliasFor);
				aliasedBy.computeIfAbsent(target, key -> new ArrayList<>()).add(attribute);
			}
		}
		return Collections.unmodifiableMap(aliasedBy);
	}

	private Method resolveAliasTarget(Method attribute, AliasFor aliasFor) {
		return resolveAliasTarget(attribute, aliasFor, true);
	}

	private Method resolveAliasTarget(Method attribute, AliasFor aliasFor, boolean checkAliasPair) {
		if (StringUtils.hasText(aliasFor.value()) && StringUtils.hasText(aliasFor.attribute())) {
			throw new AnnotationConfigurationException(String.format(
					"In @AliasFor declared on %s, attribute 'attribute' and its alias 'value' " +
					"are present with values of '%s' and '%s', but only one is permitted.",
					AttributeMethods.describe(attribute), aliasFor.attribute(),
					aliasFor.value()));
		}
		Class<? extends Annotation> targetAnnotation = aliasFor.annotation();
		if (targetAnnotation == Annotation.class) {
			targetAnnotation = this.annotationType;
		}
		String targetAttributeName = aliasFor.attribute();
		if (!StringUtils.hasLength(targetAttributeName)) {
			targetAttributeName = aliasFor.value();
		}
		if (!StringUtils.hasLength(targetAttributeName)) {
			targetAttributeName = attribute.getName();
		}
		Method target = AttributeMethods.forAnnotationType(targetAnnotation).get(targetAttributeName);
		if (target == null) {
			if (targetAnnotation == this.annotationType) {
				throw new AnnotationConfigurationException(String.format(
						"@AliasFor declaration on %s declares an alias for '%s' which is not present.",
						AttributeMethods.describe(attribute), targetAttributeName));
			}
			throw new AnnotationConfigurationException(String.format(
					"%s is declared as an @AliasFor nonexistent %s.",
					StringUtils.capitalize(AttributeMethods.describe(attribute)),
					AttributeMethods.describe(targetAnnotation, targetAttributeName)));
		}
		if (target.equals(attribute)) {
			throw new AnnotationConfigurationException(String.format(
					"@AliasFor declaration on %s points to itself. " +
					"Specify 'annotation' to point to a same-named attribute on a meta-annotation.",
					AttributeMethods.describe(attribute)));
		}
		if (!isCompatibleReturnType(attribute.getReturnType(), target.getReturnType())) {
			throw new AnnotationConfigurationException(String.format(
					"Misconfigured aliases: %s and %s must declare the same return type.",
					AttributeMethods.describe(attribute),
					AttributeMethods.describe(target)));
		}
		if (isAliasPair(target) && checkAliasPair) {
			AliasFor targetAliasFor = target.getAnnotation(AliasFor.class);
			if (targetAliasFor != null) {
				Method mirror = resolveAliasTarget(target, targetAliasFor, false);
				if (!mirror.equals(attribute)) {
					throw new AnnotationConfigurationException(String.format(
							"%s must be declared as an @AliasFor %s, not %s.",
							StringUtils.capitalize(AttributeMethods.describe(target)),
							AttributeMethods.describe(attribute), AttributeMethods.describe(mirror)));
				}
			}
		}
		return target;
	}

	private boolean isAliasPair(Method target) {
		return (this.annotationType == target.getDeclaringClass());
	}

	private boolean isCompatibleReturnType(Class<?> attributeType, Class<?> targetType) {
		return (attributeType == targetType || attributeType == targetType.getComponentType());
	}

	private void processAliases() {
		List<Method> aliases = new ArrayList<>();
		for (int i = 0; i < this.attributes.size(); i++) {
			aliases.clear();
			aliases.add(this.attributes.get(i));
			collectAliases(aliases);
			if (aliases.size() > 1) {
				processAliases(i, aliases);
			}
		}
	}

	private void collectAliases(List<Method> aliases) {
		AnnotationTypeMapping mapping = this;
		while (mapping != null) {
			int size = aliases.size();
			for (int j = 0; j < size; j++) {
				List<Method> additional = mapping.aliasedBy.get(aliases.get(j));
				if (additional != null) {
					aliases.addAll(additional);
				}
			}
			mapping = mapping.source;
		}
	}

	private void processAliases(int attributeIndex, List<Method> aliases) {
		int rootAttributeIndex = getFirstRootAttributeIndex(aliases);
		AnnotationTypeMapping mapping = this;
		while (mapping != null) {
			if (rootAttributeIndex != -1 && mapping != this.root) {
				for (int i = 0; i < mapping.attributes.size(); i++) {
					if (aliases.contains(mapping.attributes.get(i))) {
						mapping.aliasMappings[i] = rootAttributeIndex;
					}
				}
			}
			mapping.mirrorSets.updateFrom(aliases);
			mapping.claimedAliases.addAll(aliases);
			if (mapping.annotation != null) {
				int[] resolvedMirrors = mapping.mirrorSets.resolve(null,
						mapping.annotation, AnnotationUtils::invokeAnnotationMethod);
				for (int i = 0; i < mapping.attributes.size(); i++) {
					if (aliases.contains(mapping.attributes.get(i))) {
						this.annotationValueMappings[attributeIndex] = resolvedMirrors[i];
						this.annotationValueSource[attributeIndex] = mapping;
					}
				}
			}
			mapping = mapping.source;
		}
	}

	private int getFirstRootAttributeIndex(Collection<Method> aliases) {
		AttributeMethods rootAttributes = this.root.getAttributes();
		for (int i = 0; i < rootAttributes.size(); i++) {
			if (aliases.contains(rootAttributes.get(i))) {
				return i;
			}
		}
		return -1;
	}

	private void addConventionMappings() {
		if (this.distance == 0) {
			return;
		}
		AttributeMethods rootAttributes = this.root.getAttributes();
		int[] mappings = this.conventionMappings;
		Set<String> conventionMappedAttributes = new HashSet<>();
		for (int i = 0; i < mappings.length; i++) {
			String name = this.attributes.get(i).getName();
			int mapped = rootAttributes.indexOf(name);
			if (!MergedAnnotation.VALUE.equals(name) && mapped != -1 && !isExplicitAttributeOverride(name)) {
				conventionMappedAttributes.add(name);
				mappings[i] = mapped;
				MirrorSet mirrors = getMirrorSets().getAssigned(i);
				if (mirrors != null) {
					for (int j = 0; j < mirrors.size(); j++) {
						mappings[mirrors.getAttributeIndex(j)] = mapped;
					}
				}
			}
		}
		String rootAnnotationTypeName = this.root.annotationType.getName();
		String cacheKey = rootAnnotationTypeName + '-' + this.annotationType.getName();
		// We want to avoid duplicate log warnings as much as possible, without full synchronization,
		// and we intentionally invoke add() before checking if any convention-based overrides were
		// actually encountered in order to ensure that we add a "tracked" entry for the current cache
		// key in any case.
		// In addition, we do NOT want to log warnings for custom Java Bean Validation constraint
		// annotations that are meta-annotated with other constraint annotations -- for example,
		// @org.hibernate.validator.constraints.URL which overrides attributes in
		// @jakarta.validation.constraints.Pattern.
		if (conventionBasedOverrideCheckCache.add(cacheKey) && !conventionMappedAttributes.isEmpty() &&
				Arrays.stream(this.annotationType.getAnnotations()).noneMatch(isBeanValidationConstraint) &&
				logger.isWarnEnabled()) {
			logger.warn("""
					Support for convention-based annotation attribute overrides is deprecated \
					and will be removed in Spring Framework 6.2. Please annotate the following \
					attributes in @%s with appropriate @AliasFor declarations: %s"""
						.formatted(rootAnnotationTypeName, conventionMappedAttributes));
		}
	}

	/**
	 * Determine if the given annotation attribute in the {@linkplain #getRoot()
	 * root annotation} is an explicit annotation attribute override for an
	 * attribute in a meta-annotation, explicit in the sense that the override
	 * is declared via {@link AliasFor @AliasFor}.
	 * <p>If the named attribute does not exist in the root annotation, this
	 * method returns {@code false}.
	 * @param name the name of the annotation attribute to check
	 * @since 6.0
	 */
	private boolean isExplicitAttributeOverride(String name) {
		Method attribute = this.root.getAttributes().get(name);
		if (attribute != null) {
			AliasFor aliasFor = AnnotationsScanner.getDeclaredAnnotation(attribute, AliasFor.class);
			return ((aliasFor != null) &&
					(aliasFor.annotation() != Annotation.class) &&
					(aliasFor.annotation() != this.root.annotationType));
		}
		return false;
	}

	private void addConventionAnnotationValues() {
		for (int i = 0; i < this.attributes.size(); i++) {
			Method attribute = this.attributes.get(i);
			boolean isValueAttribute = MergedAnnotation.VALUE.equals(attribute.getName());
			AnnotationTypeMapping mapping = this;
			while (mapping != null && mapping.distance > 0) {
				int mapped = mapping.getAttributes().indexOf(attribute.getName());
				if (mapped != -1 && isBetterConventionAnnotationValue(i, isValueAttribute, mapping)) {
					this.annotationValueMappings[i] = mapped;
					this.annotationValueSource[i] = mapping;
				}
				mapping = mapping.source;
			}
		}
	}

	private boolean isBetterConventionAnnotationValue(int index, boolean isValueAttribute,
			AnnotationTypeMapping mapping) {

		if (this.annotationValueMappings[index] == -1) {
			return true;
		}
		int existingDistance = this.annotationValueSource[index].distance;
		return !isValueAttribute && existingDistance > mapping.distance;
	}

	@SuppressWarnings("unchecked")
	private boolean computeSynthesizableFlag(Set<Class<? extends Annotation>> visitedAnnotationTypes) {
		// Track that we have visited the current annotation type.
		visitedAnnotationTypes.add(this.annotationType);

		// Uses @AliasFor for local aliases?
		for (int index : this.aliasMappings) {
			if (index != -1) {
				return true;
			}
		}

		// Uses @AliasFor for attribute overrides in meta-annotations?
		if (!this.aliasedBy.isEmpty()) {
			return true;
		}

		// Uses convention-based attribute overrides in meta-annotations?
		for (int index : this.conventionMappings) {
			if (index != -1) {
				return true;
			}
		}

		// Has nested annotations or arrays of annotations that are synthesizable?
		if (getAttributes().hasNestedAnnotation()) {
			AttributeMethods attributeMethods = getAttributes();
			for (int i = 0; i < attributeMethods.size(); i++) {
				Method method = attributeMethods.get(i);
				Class<?> type = method.getReturnType();
				if (type.isAnnotation() || (type.isArray() && type.getComponentType().isAnnotation())) {
					Class<? extends Annotation> annotationType =
							(Class<? extends Annotation>) (type.isAnnotation() ? type : type.getComponentType());
					// Ensure we have not yet visited the current nested annotation type, in order
					// to avoid infinite recursion for JVM languages other than Java that support
					// recursive annotation definitions.
					if (visitedAnnotationTypes.add(annotationType)) {
						AnnotationTypeMapping mapping =
								AnnotationTypeMappings.forAnnotationType(annotationType, visitedAnnotationTypes).get(0);
						if (mapping.isSynthesizable()) {
							return true;
						}
					}
				}
			}
		}

		return false;
	}

	/**
	 * Method called after all mappings have been set. At this point no further
	 * lookups from child mappings will occur.
	 */
	void afterAllMappingsSet() {
		validateAllAliasesClaimed();
		for (int i = 0; i < this.mirrorSets.size(); i++) {
			validateMirrorSet(this.mirrorSets.get(i));
		}
		this.claimedAliases.clear();
	}

	private void validateAllAliasesClaimed() {
		for (int i = 0; i < this.attributes.size(); i++) {
			Method attribute = this.attributes.get(i);
			AliasFor aliasFor = AnnotationsScanner.getDeclaredAnnotation(attribute, AliasFor.class);
			if (aliasFor != null && !this.claimedAliases.contains(attribute)) {
				Method target = resolveAliasTarget(attribute, aliasFor);
				throw new AnnotationConfigurationException(String.format(
						"@AliasFor declaration on %s declares an alias for %s which is not meta-present.",
						AttributeMethods.describe(attribute), AttributeMethods.describe(target)));
			}
		}
	}

	private void validateMirrorSet(MirrorSet mirrorSet) {
		Method firstAttribute = mirrorSet.get(0);
		Object firstDefaultValue = firstAttribute.getDefaultValue();
		for (int i = 1; i <= mirrorSet.size() - 1; i++) {
			Method mirrorAttribute = mirrorSet.get(i);
			Object mirrorDefaultValue = mirrorAttribute.getDefaultValue();
			if (firstDefaultValue == null || mirrorDefaultValue == null) {
				throw new AnnotationConfigurationException(String.format(
						"Misconfigured aliases: %s and %s must declare default values.",
						AttributeMethods.describe(firstAttribute), AttributeMethods.describe(mirrorAttribute)));
			}
			if (!ObjectUtils.nullSafeEquals(firstDefaultValue, mirrorDefaultValue)) {
				throw new AnnotationConfigurationException(String.format(
						"Misconfigured aliases: %s and %s must declare the same default value.",
						AttributeMethods.describe(firstAttribute), AttributeMethods.describe(mirrorAttribute)));
			}
		}
	}

	/**
	 * Get the root mapping.
	 * @return the root mapping
	 */
	AnnotationTypeMapping getRoot() {
		return this.root;
	}

	/**
	 * Get the source of the mapping or {@code null}.
	 * @return the source of the mapping
	 */
	@Nullable
	AnnotationTypeMapping getSource() {
		return this.source;
	}

	/**
	 * Get the distance of this mapping.
	 * @return the distance of the mapping
	 */
	int getDistance() {
		return this.distance;
	}

	/**
	 * Get the type of the mapped annotation.
	 * @return the annotation type
	 */
	Class<? extends Annotation> getAnnotationType() {
		return this.annotationType;
	}

	List<Class<? extends Annotation>> getMetaTypes() {
		return this.metaTypes;
	}

	/**
	 * Get the source annotation for this mapping. This will be the
	 * meta-annotation, or {@code null} if this is the root mapping.
	 * @return the source annotation of the mapping
	 */
	@Nullable
	Annotation getAnnotation() {
		return this.annotation;
	}

	/**
	 * Get the annotation attributes for the mapping annotation type.
	 * @return the attribute methods
	 */
	AttributeMethods getAttributes() {
		return this.attributes;
	}

	/**
	 * Get the related index of an alias mapped attribute, or {@code -1} if
	 * there is no mapping. The resulting value is the index of the attribute on
	 * the root annotation that can be invoked in order to obtain the actual
	 * value.
	 * @param attributeIndex the attribute index of the source attribute
	 * @return the mapped attribute index or {@code -1}
	 */
	int getAliasMapping(int attributeIndex) {
		return this.aliasMappings[attributeIndex];
	}

	/**
	 * Get the related index of a convention mapped attribute, or {@code -1}
	 * if there is no mapping. The resulting value is the index of the attribute
	 * on the root annotation that can be invoked in order to obtain the actual
	 * value.
	 * @param attributeIndex the attribute index of the source attribute
	 * @return the mapped attribute index or {@code -1}
	 */
	int getConventionMapping(int attributeIndex) {
		return this.conventionMappings[attributeIndex];
	}

	/**
	 * Get a mapped attribute value from the most suitable
	 * {@link #getAnnotation() meta-annotation}.
	 * <p>The resulting value is obtained from the closest meta-annotation,
	 * taking into consideration both convention and alias based mapping rules.
	 * For root mappings, this method will always return {@code null}.
	 * @param attributeIndex the attribute index of the source attribute
	 * @param metaAnnotationsOnly if only meta annotations should be considered.
	 * If this parameter is {@code false} then aliases within the annotation will
	 * also be considered.
	 * @return the mapped annotation value, or {@code null}
	 */
	@Nullable
	Object getMappedAnnotationValue(int attributeIndex, boolean metaAnnotationsOnly) {
		int mappedIndex = this.annotationValueMappings[attributeIndex];
		if (mappedIndex == -1) {
			return null;
		}
		AnnotationTypeMapping source = this.annotationValueSource[attributeIndex];
		if (source == this && metaAnnotationsOnly) {
			return null;
		}
		return AnnotationUtils.invokeAnnotationMethod(source.attributes.get(mappedIndex), source.annotation);
	}

	/**
	 * Determine if the specified value is equivalent to the default value of the
	 * attribute at the given index.
	 * @param attributeIndex the attribute index of the source attribute
	 * @param value the value to check
	 * @param valueExtractor the value extractor used to extract values from any
	 * nested annotations
	 * @return {@code true} if the value is equivalent to the default value
	 */
	boolean isEquivalentToDefaultValue(int attributeIndex, Object value, ValueExtractor valueExtractor) {
		Method attribute = this.attributes.get(attributeIndex);
		return isEquivalentToDefaultValue(attribute, value, valueExtractor);
	}

	/**
	 * Get the mirror sets for this type mapping.
	 * @return the attribute mirror sets
	 */
	MirrorSets getMirrorSets() {
		return this.mirrorSets;
	}

	/**
	 * Determine if the mapped annotation is <em>synthesizable</em>.
	 * <p>Consult the documentation for {@link MergedAnnotation#synthesize()}
	 * for an explanation of what is considered synthesizable.
	 * @return {@code true} if the mapped annotation is synthesizable
	 * @since 5.2.6
	 */
	boolean isSynthesizable() {
		return this.synthesizable;
	}


	private static int[] filledIntArray(int size) {
		int[] array = new int[size];
		Arrays.fill(array, -1);
		return array;
	}

	private static boolean isEquivalentToDefaultValue(Method attribute, Object value,
			ValueExtractor valueExtractor) {

		return areEquivalent(attribute.getDefaultValue(), value, valueExtractor);
	}

	private static boolean areEquivalent(@Nullable Object value, @Nullable Object extractedValue,
			ValueExtractor valueExtractor) {

		if (ObjectUtils.nullSafeEquals(value, extractedValue)) {
			return true;
		}
		if (value instanceof Class<?> clazz && extractedValue instanceof String string) {
			return areEquivalent(clazz, string);
		}
		if (value instanceof Class<?>[] classes && extractedValue instanceof String[] strings) {
			return areEquivalent(classes, strings);
		}
		if (value instanceof Annotation annotation) {
			return areEquivalent(annotation, extractedValue, valueExtractor);
		}
		return false;
	}

	private static boolean areEquivalent(Class<?>[] value, String[] extractedValue) {
		if (value.length != extractedValue.length) {
			return false;
		}
		for (int i = 0; i < value.length; i++) {
			if (!areEquivalent(value[i], extractedValue[i])) {
				return false;
			}
		}
		return true;
	}

	private static boolean areEquivalent(Class<?> value, String extractedValue) {
		return value.getName().equals(extractedValue);
	}

	private static boolean areEquivalent(Annotation annotation, @Nullable Object extractedValue,
			ValueExtractor valueExtractor) {

		AttributeMethods attributes = AttributeMethods.forAnnotationType(annotation.annotationType());
		for (int i = 0; i < attributes.size(); i++) {
			Method attribute = attributes.get(i);
			Object value1 = AnnotationUtils.invokeAnnotationMethod(attribute, annotation);
			Object value2;
			if (extractedValue instanceof TypeMappedAnnotation<?> typeMappedAnnotation) {
				value2 = typeMappedAnnotation.getValue(attribute.getName()).orElse(null);
			}
			else {
				value2 = valueExtractor.extract(attribute, extractedValue);
			}
			if (!areEquivalent(value1, value2, valueExtractor)) {
				return false;
			}
		}
		return true;
	}


	/**
	 * A collection of {@link MirrorSet} instances that provides details of all
	 * defined mirrors.
	 */
	// 处理别名关系的“指挥部”：MirrorSets。如果说 MirrorSet 是一个具体的互助小组，那么 MirrorSets 就是管理所有小组的中心机构。
	// 负责将注解中的属性（Attributes）分配到不同的镜像组中，并最终生成一张“解析图”，告诉系统每个属性最终应该取哪个位置的值。
	// 属性归类：扫描注解属性，将所有参与了别名（Alias）关系的属性划分到对应的 MirrorSet 中。
	// 映射管理：维护一个映射表（assigned），可以快速查出某个属性属于哪个镜像组。
	// 全局解析：驱动所有内部的 MirrorSet 进行冲突检测和值仲裁，最终输出一份完整的属性索引重定向表。
	class MirrorSets {
		// 存储当前注解中所有唯一的镜像组实例。
		// 初始为空数组 EMPTY_MIRROR_SETS。
		private MirrorSet[] mirrorSets;
		// 这是一个索引对齐数组，长度与注解属性总数一致。
		private final MirrorSet[] assigned;

		MirrorSets() {
			this.assigned = new MirrorSet[attributes.size()];
			this.mirrorSets = EMPTY_MIRROR_SETS;
		}
		// 建立镜像关系的核心方法
		// 输入：一组彼此互为别名的属性方法集合（来自于对 @AliasFor 的解析）。
		void updateFrom(Collection<Method> aliases) {
			MirrorSet mirrorSet = null;
			int size = 0;
			int last = -1;
			for (int i = 0; i < attributes.size(); i++) {
				// 遍历所有属性，寻找属于 aliases 集合的属性。
				Method attribute = attributes.get(i);
				if (aliases.contains(attribute)) {
					size++;
					if (size > 1) {
						// 动态创建组：当发现第二个及以上的关联属性时，创建一个新的 MirrorSet。
						if (mirrorSet == null) {
							mirrorSet = new MirrorSet();
							this.assigned[last] = mirrorSet;
						}
						// 双向绑定：将这些属性在 assigned 数组中的对应位置指向该 MirrorSet。
						this.assigned[i] = mirrorSet;
					}
					last = i;
				}
			}
			if (mirrorSet != null) {
				// 将这些属性在 assigned 数组中的对应位置指向该 MirrorSet
				mirrorSet.update();
				// 利用 LinkedHashSet 对 assigned 中的组进行去重
				Set<MirrorSet> unique = new LinkedHashSet<>(Arrays.asList(this.assigned));
				unique.remove(null);
				// 更新 mirrorSets 数组
				this.mirrorSets = unique.toArray(EMPTY_MIRROR_SETS);
			}
		}

		int size() {
			return this.mirrorSets.length;
		}

		MirrorSet get(int index) {
			return this.mirrorSets[index];
		}
		// 查找指定索引的属性是否被分配到了某个镜像组。这是外界查询属性归属的关键入口。
		@Nullable
		MirrorSet getAssigned(int attributeIndex) {
			return this.assigned[attributeIndex];
		}
		// 生成最终解析结果的方法，返回一个 int[] 索引表。
		int[] resolve(@Nullable Object source, @Nullable Object annotation, ValueExtractor valueExtractor) {
			// 初始化索引表：默认 result[i] = i，即每个属性指向它自己。
			int[] result = new int[attributes.size()];
			for (int i = 0; i < result.length; i++) {
				result[i] = i;
			}
			for (int i = 0; i < size(); i++) {
				MirrorSet mirrorSet = get(i);
				// 组内仲裁：遍历每一个镜像组（MirrorSet），调用其 resolve 方法（之前解析过的冲突检测和选值逻辑）
				int resolved = mirrorSet.resolve(source, annotation, valueExtractor);
				// 索引重定向：如果 MirrorSet 决定该组最终生效的是索引为 K 的属性，那么该组内所有成员在 result 表中的值都会被设置为 K。
				for (int j = 0; j < mirrorSet.size; j++) {
					result[mirrorSet.indexes[j]] = resolved;
				}
			}
			return result;
		}


		/**
		 * A single set of mirror attributes.
		 */
		// Spring 处理注解“镜像属性”的核心算法所在。
		// 在 Spring 注解模型中，镜像（Mirror） 是指两个或多个属性通过 @AliasFor 互为别名。
		// 例如，在 @RequestMapping 中，value 属性和 path 属性互为镜像。
		// MirrorSet 的主要作用是：
		// 编组：将所有互为别名的属性归为一组。
		// 冲突检测：在运行时检查用户是否为同一组镜像属性设置了不同的值。
		// 结果仲裁（Resolve）：当一组镜像属性中有的设置了值，有的使用了默认值时，决定最终应该使用哪一个属性的值。
		class MirrorSet {
			// 作用：记录当前镜像组中包含的属性数量。
			private int size;
			// 存储属于该镜像组的属性在 attributes 列表中的索引位置。
			// 初始化：长度固定为总属性数量，但在 update 方法中会被压缩有效长度。
			private final int[] indexes = new int[attributes.size()];
			// 同步索引状态。
			// 遍历所属 MirrorSets 外部类的 assigned 数组（该数组记录了每个属性属于哪个 MirrorSet），
			// 将指向当前对象的属性索引提取并存入自己的 indexes 数组中。
			void update() {
				this.size = 0;
				Arrays.fill(this.indexes, -1);
				for (int i = 0; i < MirrorSets.this.assigned.length; i++) {
					if (MirrorSets.this.assigned[i] == this) {
						this.indexes[this.size] = i;
						this.size++;
					}
				}
			}
			// 该类最重要的逻辑方法
			// 用于在多个镜像属性中“选出”最终生效的那一个。
			<A> int resolve(@Nullable Object source, @Nullable A annotation, ValueExtractor valueExtractor) {
				int result = -1;
				Object lastValue = null;
				for (int i = 0; i < this.size; i++) {
					// 循环处理该组内所有的属性索引
					Method attribute = attributes.get(this.indexes[i]);
					// 提取值：使用 valueExtractor 从原始数据（注解或 Map）中提取该属性的当前值。
					Object value = valueExtractor.extract(attribute, annotation);
					// 通过 isEquivalentToDefaultValue 判断该值是否为初始默认值。
					boolean isDefaultValue = (value == null ||
							isEquivalentToDefaultValue(attribute, value, valueExtractor));

					if (isDefaultValue || ObjectUtils.nullSafeEquals(lastValue, value)) {
						if (result == -1) {
							result = this.indexes[i];
						}
						continue;
					}
					// 如果发现两个非默认值且不相等（例如用户同时设置了 value="/a" 和 path="/b"），则抛出 AnnotationConfigurationException。
					// 这正是 Spring 报错“Different @AliasFor mirror values...”的源头。
					if (lastValue != null && !ObjectUtils.nullSafeEquals(lastValue, value)) {
						String on = (source != null) ? " declared on " + source : "";
						throw new AnnotationConfigurationException(String.format(
								"Different @AliasFor mirror values for annotation [%s]%s; attribute '%s' " +
								"and its alias '%s' are declared with values of [%s] and [%s].",
								getAnnotationType().getName(), on,
								attributes.get(result).getName(),
								attribute.getName(),
								ObjectUtils.nullSafeToString(lastValue),
								ObjectUtils.nullSafeToString(value)));
					}
					result = this.indexes[i];
					lastValue = value;
				}
				return result;
			}

			int size() {
				return this.size;
			}

			Method get(int index) {
				int attributeIndex = this.indexes[index];
				return attributes.get(attributeIndex);
			}

			int getAttributeIndex(int index) {
				return this.indexes[index];
			}
		}
	}

}
