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

import org.springframework.expression.Expression;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.DataBindingPropertyAccessor;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A class demonstrating safe and unsafe use of SpEL when composing expressions from user input
 *
 * @author Josh Cummings
 */
@RestController
@RequestMapping("/widget")
public class WidgetController {
	private final SpelExpressionParser parser = new SpelExpressionParser();

	private final WidgetRepository repo;

	public WidgetController(WidgetRepository repo) {
		this.repo = repo;
	}

	@GetMapping("/search")
	public Set<Widget> findWidgets(@RequestParam("term") String term) {
		return repo.findByNameContaining(term);
	}

	@GetMapping("/unsafe-search")
	public Set<Widget> unsafeFindWidgetsAdvancedly(@RequestParam("term") String term,
												   @RequestParam("filter") String filter) {

		Set<Widget> widgets = this.repo.findByNameContaining(term);
		StandardEvaluationContext context = new StandardEvaluationContext(widgets);
		Expression expression = this.parser.parseExpression("#this.?[enabled and (" + filter + ")]");
		return new LinkedHashSet<>((Collection<Widget>)expression.getValue(context));
	}

	@GetMapping("/safer-search")
	public Set<Widget> saferFindWidgetsAdvancedly(@RequestParam("term") String term,
												  @RequestParam("filter") String filter) {

		Set<Widget> widgets = this.repo.findByNameContaining(term);

		// SimpleEvaluationContext takes a whitelist approach, requiring the code to explicitly enable
		// bean resolution, property access, and type resolution

		// In this case, the code is only allowing read access to properties derivable from the context
		SimpleEvaluationContext context =
				new SimpleEvaluationContext.Builder(DataBindingPropertyAccessor.forReadOnlyAccess())
					.withRootObject(widgets).build();

		// still though, query composition is a dangerous business
		filter = "#this.?[enabled and (" + filter + ")]";
		Expression expression = this.parser.parseExpression(filter);
		return new LinkedHashSet<>((Collection<Widget>)expression.getValue(context));
	}

	@GetMapping("/safest-search")
	public Set<Widget> safestFindWidgetsAdvancedly(@RequestParam("term") String term,
												   @RequestParam("filter") String filter) {

		Set<Widget> widgets = this.repo.findByNameContaining(term);
		SimpleEvaluationContext context =
				new SimpleEvaluationContext.Builder(DataBindingPropertyAccessor.forReadOnlyAccess())
					.withRootObject(widgets).build();

		// whitelisting the incoming filter forms a water-tight trust boundary around what gets sent to the expression
		Pattern alphaNumeric = Pattern.compile("[A-Za-z0-9\\.\\s]+");

		if ( alphaNumeric.matcher(filter).matches() ) {
			Expression expression = this.parser.parseExpression("#this.?[enabled and (" + filter + ")]");
			return new LinkedHashSet<>((Collection<Widget>) expression.getValue(context));
		} else {
			throw new IllegalArgumentException("filter contains invalid charaters");
		}

		// alternatively, encoding could be an option for more complex needs
	}

	@ExceptionHandler({ SpelEvaluationException.class, IllegalArgumentException.class })
	public ResponseEntity<?> badRequest() {
		return ResponseEntity.badRequest().build();
	}
}
