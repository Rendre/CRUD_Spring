package com.game.service;

import com.game.controller.RequestPlayerDTO;
import com.game.controller.ResponsePlayerDTO;
import java.util.List;

public interface IPlayerService {

     Integer getPlayerCounts(RequestPlayerDTO requestPlayerDTO);

     List<ResponsePlayerDTO> getPlayerList(RequestPlayerDTO requestPlayerDTO);

     ResponsePlayerDTO update(int id, RequestPlayerDTO requestPlayerDTO);

     ResponsePlayerDTO save(RequestPlayerDTO requestPlayerDTO);

     void deleteById(int id);

     ResponsePlayerDTO findById(int id);

}
