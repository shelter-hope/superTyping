package com.typinggame.common.admin;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * 后台列表查询条件。值为 null 或空白字符串的条件会被忽略，
 * 字段支持 "user.userId" 这样的关联路径。
 */
public class AdminFilter<T> implements Specification<T> {

    private final transient List<BiFunction<Root<T>, CriteriaBuilder, Predicate>> parts = new ArrayList<>();

    public static <T> AdminFilter<T> of(Class<T> type) {
        return new AdminFilter<>();
    }

    public AdminFilter<T> eq(String field, Object value) {
        if (present(value)) {
            parts.add((root, cb) -> cb.equal(path(root, field), value));
        }
        return this;
    }

    public AdminFilter<T> like(String field, String value) {
        if (present(value)) {
            String pattern = "%" + value.trim().toLowerCase() + "%";
            parts.add((root, cb) -> cb.like(cb.lower(path(root, field).as(String.class)), pattern));
        }
        return this;
    }

    public <V extends Comparable<? super V>> AdminFilter<T> between(String field, V fromInclusive, V toExclusive) {
        if (fromInclusive != null) {
            parts.add((root, cb) -> cb.greaterThanOrEqualTo(path(root, field), fromInclusive));
        }
        if (toExclusive != null) {
            parts.add((root, cb) -> cb.lessThan(path(root, field), toExclusive));
        }
        return this;
    }

    public int size() {
        return parts.size();
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Predicate[] predicates = parts.stream().map(p -> p.apply(root, cb)).toArray(Predicate[]::new);
        return cb.and(predicates);
    }

    private static boolean present(Object value) {
        if (value == null) return false;
        return !(value instanceof String s) || !s.isBlank();
    }

    private static <Y> Path<Y> path(Root<?> root, String field) {
        Path<?> p = root;
        for (String segment : field.split("\\.")) {
            p = p.get(segment);
        }
        @SuppressWarnings("unchecked")
        Path<Y> typed = (Path<Y>) p;
        return typed;
    }
}
