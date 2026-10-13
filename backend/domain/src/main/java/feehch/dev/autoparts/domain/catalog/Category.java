package feehch.dev.autoparts.domain.catalog;

import feehch.dev.autoparts.domain.common.DomainException;
import feehch.dev.autoparts.domain.common.Guard;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Категория каталога запчастей. Категории образуют дерево через {@code parentId}.
 */
public class Category implements Serializable {

    private static final Map<Character, String> TRANSLIT = Map.ofEntries(
            Map.entry('а', "a"), Map.entry('б', "b"), Map.entry('в', "v"), Map.entry('г', "g"),
            Map.entry('д', "d"), Map.entry('е', "e"), Map.entry('ё', "e"), Map.entry('ж', "zh"),
            Map.entry('з', "z"), Map.entry('и', "i"), Map.entry('й', "y"), Map.entry('к', "k"),
            Map.entry('л', "l"), Map.entry('м', "m"), Map.entry('н', "n"), Map.entry('о', "o"),
            Map.entry('п', "p"), Map.entry('р', "r"), Map.entry('с', "s"), Map.entry('т', "t"),
            Map.entry('у', "u"), Map.entry('ф', "f"), Map.entry('х', "h"), Map.entry('ц', "ts"),
            Map.entry('ч', "ch"), Map.entry('ш', "sh"), Map.entry('щ', "sch"), Map.entry('ъ', ""),
            Map.entry('ы', "y"), Map.entry('ь', ""), Map.entry('э', "e"), Map.entry('ю', "yu"),
            Map.entry('я', "ya"));

    private Long id;
    private String name;
    private String slug;
    private String description;
    private Long parentId;

    public Category(Long id, String name, String slug, String description, Long parentId) {
        this.id = id;
        this.name = Guard.notBlank(name, "название");
        this.slug = slug == null || slug.isBlank() ? slugify(this.name) : slugify(slug);
        this.description = Guard.trimToNull(description);
        this.parentId = parentId;
    }

    public void update(String name, String slug, String description, Long parentId) {
        this.name = Guard.notBlank(name, "название");
        this.slug = slug == null || slug.isBlank() ? slugify(this.name) : slugify(slug);
        this.description = Guard.trimToNull(description);
        this.parentId = parentId;
    }

    /**
     * Проверяет, что назначение родителя не создаёт цикл в дереве категорий.
     *
     * @param all все категории каталога
     */
    public void ensureNoCycle(Collection<Category> all) {
        if (id == null || parentId == null) {
            return;
        }
        Map<Long, Long> parents = new java.util.HashMap<>();
        all.forEach(c -> parents.put(c.getId(), c.getParentId()));
        parents.put(id, parentId);
        Set<Long> visited = new HashSet<>();
        Long current = parentId;
        while (current != null) {
            if (Objects.equals(current, id) || !visited.add(current)) {
                throw new DomainException("Категория не может быть вложена сама в себя");
            }
            current = parents.get(current);
        }
    }

    /**
     * Идентификаторы категории и всех её потомков.
     */
    public static Set<Long> withDescendants(Long rootId, Collection<Category> all) {
        Set<Long> result = new HashSet<>();
        result.add(rootId);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Category c : all) {
                if (c.getParentId() != null && result.contains(c.getParentId()) && result.add(c.getId())) {
                    changed = true;
                }
            }
        }
        return result;
    }

    public static String slugify(String source) {
        StringBuilder sb = new StringBuilder();
        for (char ch : source.trim().toLowerCase(Locale.ROOT).toCharArray()) {
            if (TRANSLIT.containsKey(ch)) {
                sb.append(TRANSLIT.get(ch));
            } else if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')) {
                sb.append(ch);
            } else {
                sb.append('-');
            }
        }
        String slug = sb.toString().replaceAll("-{2,}", "-").replaceAll("(^-)|(-$)", "");
        if (slug.isEmpty()) {
            throw new DomainException("Не удалось сформировать slug категории");
        }
        return slug;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public String getDescription() {
        return description;
    }

    public Long getParentId() {
        return parentId;
    }
}
