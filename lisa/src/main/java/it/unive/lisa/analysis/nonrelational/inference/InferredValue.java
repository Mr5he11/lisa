package it.unive.lisa.analysis.nonrelational.inference;

import it.unive.lisa.analysis.nonrelational.NonRelationalDomain;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;

/**
 * A value that can be inferred by {@link InferenceSystem}s. This builds on top
 * of {@link NonRelationalDomain}, adding {@link #variable(Identifier)} to force
 * information stored into variables to predetermined data if needed.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the concrete type of inferred value
 */
public interface InferredValue<T extends InferredValue<T>>
		extends NonRelationalDomain<T, ValueExpression, InferenceSystem<T>> {

	/**
	 * Yields a fixed abstraction of the given variable. The abstraction does
	 * not depend on the abstract values that get assigned to the variable, but
	 * is instead fixed among all possible execution paths. If this method does
	 * not return the bottom element (as the default implementation does), then
	 * {@link InferenceSystem#assign(Identifier, it.unive.lisa.symbolic.SymbolicExpression)}
	 * will store that abstract element instead of the one computed starting
	 * from the expression.
	 * 
	 * @param id The identifier representing the variable being assigned
	 * 
	 * @return the fixed abstraction of the variable
	 */
	default T variable(Identifier id) {
		return bottom();
	}
}
