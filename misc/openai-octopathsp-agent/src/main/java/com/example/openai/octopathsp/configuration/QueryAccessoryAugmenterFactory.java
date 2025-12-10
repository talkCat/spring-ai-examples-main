package com.example.openai.octopathsp.configuration;


import com.example.openai.octopathsp.prompt.PromptTemplateCustomize;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;

/**
 * 自定义检索增强器 饰品
 */
@Slf4j
public class QueryAccessoryAugmenterFactory {


    /**
     * 获取默认的QueryAugmenter，约束大模型必须基于检索文本回答，不能基于以往知识
     *
     * @return QueryAugmenter
     */
    public static QueryAugmenter getDefault() {
        return ContextualQueryAugmenter.builder().build();
    }

    /**
     * 获取基于上下文检索的QueryAugmenter，大模型结合检索文本和以往知识来扩充回答
     *
     * @return QueryAugmenter
     */
    public static QueryAugmenter getContextBasedQueryAugmenter() {
        /*PromptTemplate promptTemplate = new PromptTemplate(
                "Context information is below.\n\n---------------------\n{context}\n---------------------\n\nGiven the context information, answer the query.\n\nFollow these rules:\n\n1. Avoid statements like \"Based on the context...\" or \"The provided information...\".\n\nQuery: {query}\n\nAnswer:\n");*/

        //基于知识库存在的情况
        PromptTemplate promptTemplate = new PromptTemplate(PromptTemplateCustomize.CONTEXT_PROMPT_TEMPLATE_ACCESSORY_RAG);
        //基于知识库不存在的情况
        PromptTemplate promptTemplateEmpty = new PromptTemplate(PromptTemplateCustomize.EMPTY_CONTEXT_PROMPT_TEMPLATE);
        return ContextualQueryAugmenter.builder()
                .promptTemplate(promptTemplate)
                .emptyContextPromptTemplate(promptTemplateEmpty)
                //.allowEmptyContext(true)
                .build();
    }
}
