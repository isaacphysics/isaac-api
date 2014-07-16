package uk.ac.cam.cl.dtg.segue.api;

import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.impl.DefaultMapperFactory;

public class DOAndDTOMapper {

	private MapperFacade mapper;
	
	public DOAndDTOMapper() {
		MapperFactory mapperFactory = new DefaultMapperFactory.Builder().build();
		mapper = mapperFactory.getMapperFacade();
	}

	/**
	 * Gets the mapper.
	 * @return the mapper
	 */
	public final MapperFacade getMapper() {
		return mapper;
	}

	/**
	 * Sets the mapper.
	 * @param mapper the mapper to set
	 */
	public final void setMapper(MapperFacade mapper) {
		this.mapper = mapper;
	}
	
	
}
