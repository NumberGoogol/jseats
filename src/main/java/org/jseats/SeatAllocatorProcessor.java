package org.jseats;

import java.util.List;
import java.util.Properties;

import org.jseats.model.Result;
import org.jseats.model.Result.ResultType;
import org.jseats.model.ResultDecorator;
import org.jseats.model.SeatAllocationException;
import org.jseats.model.SeatAllocationMethod;
import org.jseats.model.Tally;
import org.jseats.model.TallyFilter;
import org.jseats.model.tie.TieBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeatAllocatorProcessor {

	Logger log = LoggerFactory.getLogger(SeatAllocatorProcessor.class);

	ProcessorConfig config;

	SeatAllocatorResolver resolver;

	/*
	 * Configuration
	 */

	public SeatAllocatorProcessor(ProcessorConfig config,
			SeatAllocatorResolver resolver) {
		log.debug("Initializing processor with provided configuration and resolver.");
		this.config = config;
		this.resolver = resolver;
	}

	public SeatAllocatorProcessor(ProcessorConfig config) {
		log.debug("Initializing processor with provided configuration and default resolver.");
		this.config = config;
		this.resolver = new SeatAllocatorDefaultResolver();
	}

	public SeatAllocatorProcessor() {
		log.debug("Initializing processor with default configuration and resolver.");
		this.config = new ProcessorConfig();
		this.resolver = new SeatAllocatorDefaultResolver();
	}

	public ProcessorConfig getConfig() {
		return config;
	}

	public SeatAllocatorResolver getResolver() {
		return resolver;
	}

	/*
	 * Tally
	 */

	public Tally getTally() {
		return config.getTally();
	}

	public void setTally(Tally tally) {
		config.setTally(tally);
	}

	public boolean addTallyFilter(TallyFilter filter) {
		return config.getTallyFilters().add(filter);
	}

	public List<TallyFilter> getTallyFilters() {
		return config.getTallyFilters();
	}

	public boolean removeTallyFilter(TallyFilter filter) {
		return config.getTallyFilters().remove(filter);
	}

	/*
	 * Result
	 */

	public boolean addResultDecorator(ResultDecorator decorator) {
		return config.getResultDecorator().add(decorator);
	}

	public List<ResultDecorator> getResultDecorator() {
		return config.getResultDecorator();
	}

	public boolean removeResultDecorator(ResultDecorator decorator) {
		return config.getResultDecorator().remove(decorator);
	}

	public TieBreaker getTieBreaker() {
		return config.getTieBreaker();
	}

	public void setTieBreaker(TieBreaker tieBreaker) {
		config.setTieBreaker(tieBreaker);
	}

	/*
	 * Properties
	 */

	public void setProperty(String key, String value) {
		log.debug("Set property " + key + "=" + value);
		config.getProperties().setProperty(key, value);
	}

	public String getProperty(String key) {
		return config.getProperties().getProperty(key);
	}

	public Properties getProperties() {
		return config.getProperties();
	}

	/*
	 * Method
	 */

	public void setMethodByName(String method) throws SeatAllocationException {

		log.debug("Adding method by name:" + method);
		config.setMethod(resolver.resolveMethod(method));
		config.setMethodName(method);
	}

	public void setMethodByClass(Class<? extends SeatAllocationMethod> clazz)
			throws InstantiationException, IllegalAccessException {

		log.debug("Adding method by class:" + clazz);
		config.setMethod(clazz.newInstance());
	}

	public void setMethod(SeatAllocationMethod method) {
		log.debug("Adding method by instance:" + method);
		config.setMethod(method);
	}

	/*
	 * Processor
	 */

	public void reset() {
		log.debug("Resetting processor");
		config.reset();
	}

	public Result process() throws SeatAllocationException {

		if (config.getTally() == null)
			throw new SeatAllocationException(
					"Trying to run processor without providing a tally");

		log.trace("Executing processor.");
		log.debug(config.toString());

		if (!config.getTallyFilters().isEmpty()) {
			log.trace("Executing filters");
			for (TallyFilter filter : config.getTallyFilters()) {
				log.debug("Executing filter: " + filter);
				config.setTally(filter.filter(config.getTally()));
			}
		}
		boolean candidatesExist =   (config.getTally().getCandidates().size() < 1);
		boolean candidatesWithoutVotes = !config.getTally().getCandidates().stream().anyMatch(c -> c.getVotes()> 0);
		
		Result result = null;
				
		if (candidatesExist){
			result = new Result(ResultType.NO_CANDIDATES_INPUTTED);
		} 
		else if (candidatesWithoutVotes){
			result = new Result(ResultType.CANDIDATES_NO_VOTES);
		} else {
			result = config.getMethod().process(config.getTally(), config.getProperties(), config.getTieBreaker());
			log.trace("Processed");
		}
		
		if (!config.getResultDecorator().isEmpty()) {
			log.trace("Executing decorators");
			for (ResultDecorator decorator : config.getResultDecorator()) {
				log.debug("Executing decorator: " + decorator);
				result = decorator.decorate(result);
			}
		}

		return result;
	}
}
