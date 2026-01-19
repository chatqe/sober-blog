package io.charqe.qs.modules.category.service;

import io.charqe.qs.modules.category.domain.dao.CategoryMapper;
import io.charqe.qs.modules.category.domain.pojo.Category;
import io.charqe.qs.common.model.mp.CoreServiceImpl;
import org.springframework.stereotype.Service;

/**
 * *
 *
 * @author Joy
 * @since 2026-01-17 10:47
 */
@Service
public class CategoryServiceImpl extends CoreServiceImpl<CategoryMapper, Category> implements CategoryService {

}
