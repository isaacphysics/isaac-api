package uk.ac.cam.cl.dtg.segue.api;

import org.modelmapper.ModelMapper;

public class DOAndDTOMapper {

	private ModelMapper mapper;
	
	public DOAndDTOMapper() {
		this.mapper = new ModelMapper();
		
	}

	/**
	 * Gets the mapper.
	 * @return the mapper
	 */
	public final ModelMapper getMapper() {
		return mapper;
	}

	/**
	 * Sets the mapper.
	 * @param mapper the mapper to set
	 */
	public final void setMapper(ModelMapper mapper) {
		this.mapper = mapper;
	}
	
	
}
