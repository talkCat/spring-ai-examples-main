package com.example.openai.streaming.prompt;

/**
 * @author n039920
 * @date 2025/11/12
 * @description TODO
 */
public interface PromptTemplateCustomize {

    static final String SYSTEM_PROMPT = """
            你是一个有用的人工智能助手，可以帮助人们查找信息,请在回答的末尾给予人们美好的祝福。
            """;

    //基于知识库不存在的情况
    static final String EMPTY_CONTEXT_PROMPT_TEMPLATE = """
                         请注意：对于以下情况，请直接回答而不要提及知识库：
                         - 问候（如“你好”）
                         - 闲聊（如“你叫什么名字”）
                         - 通用问题（如“地球是圆的吗”）
                         对于其他问题，如果它们需要特定领域的专业知识，而你没有相关信息，请礼貌告知无法回答。
            """;

    //基于知识库存在的情况
    static String CONTEXT_PROMPT_TEMPLATE_RAG = """
            上下文信息如下。

            ---------------------
            {context}
            ---------------------

            给定上下文信息，回答查询。

            请遵循以下规则：

            避免使用“基于上下文…”或“所提供的信息…”这样的表述。

            查询：｛query｝

            答案：

            """;

}