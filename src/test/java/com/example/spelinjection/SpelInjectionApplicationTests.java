/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.spelinjection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SpelInjectionApplicationTests {

	@Autowired
	private WebApplicationContext context;

	MockMvc mvc;

	SpelExpressionParser parser = new SpelExpressionParser();

	@Before
	public void setup() {
		mvc = MockMvcBuilders
				.webAppContextSetup(context)
				.build();
	}

	/*
	  The following three tests show that unsafe-search allows calls to Runtime as well as allows user input to
	  break its query boundaries.
	 */

	@Test
	public void performWhenUnsafeSearchUsesJustPriceThenOk() throws Exception {
		this.mvc.perform(
				get("/widget/unsafe-search")
						.param("term", "Widget")
						.param("filter", "price gt 0")
		).andExpect(status().isOk());
	}

	@Test
	public void performWhenUnsafeSearchUsesSpelCharactersThenStillExecutes() throws Exception {
		this.mvc.perform(
				get("/widget/unsafe-search")
						.param("term", "Widget")
						.param("filter", "true)] != null ? #this : #this.?[(true")
		).andExpect(status().isOk());
	}

	@Test
	public void performWhenUnsafeSearchUsesRuntimeThenStillExecutes() throws Exception {
		this.mvc.perform(
				get("/widget/unsafe-search")
					.param("term", "Widget")
					.param("filter", "price gt 0 and T(Runtime).getRuntime().exec(\"pwd\").waitFor() == 0")
		).andExpect(status().isOk());
	}

	/*
	  The following three tests show that safer-search disallows calls to Runtime but still allows user input to
	  break its query boundaries.
	 */

	@Test
	public void performWhenSaferSearchUsesJustPriceThenOk() throws Exception {
		this.mvc.perform(
				get("/widget/safer-search")
						.param("term", "Widget")
						.param("filter", "price gt 0")
		).andExpect(status().isOk());
	}

	@Test
	public void performWhenSaferSearchUsesSpelCharactersThenStillExecutes() throws Exception {
		this.mvc.perform(
				get("/widget/safer-search")
						.param("term", "Widget")
						.param("filter", "true)] != null ? #this : #this.?[(true")
		).andExpect(status().isOk());
	}

	@Test
	public void performWhenSaferSearchUsesRuntimeThenBadRequest() throws Exception {
		this.mvc.perform(
				get("/widget/safer-search")
						.param("term", "Widget")
						.param("filter", "price gt 0 and T(Runtime).getRuntime().exec(\"pwd\").waitFor() == 0")
		).andExpect(status().isBadRequest());
	}

	/*
	  The following three tests show that safest-search disallows both calls to Runtime and guards its query
	  boundaries
	 */

	@Test
	public void performWhenSafestSearchUsesJustPriceThenOk() throws Exception {
		this.mvc.perform(
				get("/widget/safest-search")
						.param("term", "Widget")
						.param("filter", "price gt 0")
		).andExpect(status().isOk());
	}

	@Test
	public void performWhenSafestSearchUsesSpelCharactersThenBadRequest() throws Exception {
		this.mvc.perform(
				get("/widget/safest-search")
						.param("term", "Widget")
						.param("filter", "true)] != null ? #this : #this.?[(true")
		).andExpect(status().isBadRequest());
	}

	@Test
	public void performWhenSafestSearchUsesRuntimeThenBadRequest() throws Exception {
		this.mvc.perform(
				get("/widget/safest-search")
						.param("term", "Widget")
						.param("filter", "price gt 0 and T(Runtime).getRuntime().exec(\"pwd\").waitFor() == 0")
		).andExpect(status().isBadRequest());
	}

	@Test
	public void performWhenImpermeableSearchUsesRuntimeThenBadRequest() throws Exception {
		this.mvc.perform(
				get("/widget/impermeable-search")
						.param("term", "Widget")
						.param("min-price", "price gt 0 and T(Runtime).getRuntime().exec(\"pwd\").waitFor() == 0")
						.param("max-price", "price gt 0 and T(Runtime).getRuntime().exec(\"pwd\").waitFor() == 0")
		).andExpect(status().isBadRequest());
	}

	@Test
	public void performWhenImpermeableSearchUsesJustPriceThenOk() throws Exception {
		this.mvc.perform(
				get("/widget/impermeable-search")
						.param("term", "Widget")
						.param("min-price", "5")
						.param("max-price", "20")
		).andExpect(status().isOk());
	}
}
