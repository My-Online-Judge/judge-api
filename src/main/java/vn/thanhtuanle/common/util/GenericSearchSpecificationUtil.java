package vn.thanhtuanle.common.util;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import jakarta.persistence.criteria.*;
import java.util.*;

public class GenericSearchSpecificationUtil<T> implements Specification<T> {

    private final Map<String, Object> filters;
    private final List<String> searchFields;
    private final String searchValue;

    /**
     * Constructor
     *
     * @param filters      Map chứa các bộ lọc exact match (status, difficulty,
     *                     category.id, ...)
     * @param searchFields Danh sách các field muốn tìm kiếm full-text (LIKE
     *                     %value%)
     *                     Ví dụ: Arrays.asList("title", "description", "code")
     * @param searchValue  Từ khóa tìm kiếm (nếu null hoặc empty → không áp dụng
     *                     search)
     */
    public GenericSearchSpecificationUtil(
            Map<String, Object> filters,
            List<String> searchFields,
            String searchValue) {

        this.filters = filters != null ? filters : Collections.emptyMap();
        this.searchFields = searchFields != null ? searchFields : Collections.emptyList();
        this.searchValue = searchValue;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();

        // 1. Full-text search trên nhiều field (OR giữa các field)
        if (StringUtils.hasText(searchValue) && !searchFields.isEmpty()) {
            String pattern = "%" + searchValue.trim().toLowerCase() + "%";
            List<Predicate> searchPredicates = new ArrayList<>();

            for (String field : searchFields) {
                if (StringUtils.hasText(field)) {
                    Path<String> path = getPath(root, field);
                    searchPredicates.add(cb.like(cb.lower(path), pattern));
                }
            }

            if (!searchPredicates.isEmpty()) {
                predicates.add(cb.or(searchPredicates.toArray(new Predicate[0])));
            }
        }

        // 2. Exact filters (equal)
        filters.forEach((field, value) -> {
            if (value != null) {
                Path<Object> path = getPath(root, field);
                if (value instanceof String str && StringUtils.hasText(str)) {
                    predicates.add(cb.equal(path, str.trim()));
                } else {
                    predicates.add(cb.equal(path, value));
                }
            }
        });

        // Nếu không có predicate nào → trả về điều kiện luôn đúng
        if (predicates.isEmpty()) {
            return cb.conjunction();
        }

        return cb.and(predicates.toArray(new Predicate[0]));
    }

    /**
     * Hỗ trợ truy cập nested field (ví dụ: "category.name", "author.fullName")
     */
    @SuppressWarnings("unchecked")
    private <Y> Path<Y> getPath(Root<T> root, String fieldName) {
        if (!StringUtils.hasText(fieldName)) {
            throw new IllegalArgumentException("Field name cannot be empty");
        }

        String[] parts = fieldName.split("\\.");
        Path<?> path = root;
        for (String part : parts) {
            path = path.get(part.trim());
        }
        return (Path<Y>) path;
    }
}