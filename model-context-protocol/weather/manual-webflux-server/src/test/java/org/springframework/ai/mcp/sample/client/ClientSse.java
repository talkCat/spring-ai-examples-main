/*
* Copyright 2024 - 2024 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.springframework.ai.mcp.sample.client;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;

import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Map;

/**
 * @author Christian Tzolov
 */
public class ClientSse {

	public static void main(String[] args) {
		var transport = new WebFluxSseClientTransport(WebClient.builder().baseUrl("http://10.20.40.246:8080"));

		//new SampleClient(transport).run();

		run(transport);
	}

	public static void run(WebFluxSseClientTransport transport) {

		var client = McpClient.sync(transport).build();

		client.initialize();

		client.ping();

		// List and demonstrate tools
		McpSchema.ListToolsResult toolsList = client.listTools();
		System.out.println("Available Tools = " + toolsList);

		McpSchema.CallToolResult weatherForcastResult = client.callTool(new McpSchema.CallToolRequest("getWeatherForecastByLocation",
				Map.of("latitude", "47.6062", "longitude", "-122.3321")));
		System.out.println("Weather Forcast: " + weatherForcastResult);

		McpSchema.CallToolResult alertResult = client.callTool(new McpSchema.CallToolRequest("getAlerts", Map.of("state", "NY")));
		System.out.println("Alert Response = " + alertResult);

		// 查询人员信息
		McpSchema.CallToolResult personResult = client.callTool(new McpSchema.CallToolRequest("getPersonInfo", Map.of("name", "张三")));
		System.out.println("Person Info: " + personResult);

		client.closeGracefully();

	}

}
