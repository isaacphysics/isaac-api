package uk.ac.cam.cl.dtg.util.mappers;


import org.mapstruct.Mapper;
import uk.ac.cam.cl.dtg.isaac.dos.GameboardDO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;

@Mapper
public interface GameboardMapper {
    GameboardDTO map(GameboardDO source);
    GameboardDO map(GameboardDTO source);
}
