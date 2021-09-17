package com.game.controller;

import com.game.service.IPlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/rest/players")
public class PlayerController {

    private IPlayerService iPlayerService;

    @Autowired
    public PlayerController(IPlayerService iPlayerService) {
        this.iPlayerService = iPlayerService;
    }


    @GetMapping("/count")
    public Integer getPlayersCount(@ModelAttribute("requestPlayerDTO") RequestPlayerDTO requestPlayerDTO){
        Integer count = iPlayerService.getPlayerCounts(requestPlayerDTO);
        return count;
    }

    @GetMapping()
    public List<ResponsePlayerDTO> getPlayerList(@ModelAttribute("requestPlayerDTO") RequestPlayerDTO requestPlayerDTO){
        List<ResponsePlayerDTO> players = iPlayerService.getPlayerList(requestPlayerDTO);
        return players;
    }

    @PostMapping("{id}")
    public ResponsePlayerDTO update(@PathVariable("id") int id, @RequestBody() RequestPlayerDTO requestPlayerDTO){
        ResponsePlayerDTO responsePlayerDTO = iPlayerService.update(id, requestPlayerDTO);
        return responsePlayerDTO;
    }

    @PostMapping()
    public ResponsePlayerDTO create(@RequestBody() RequestPlayerDTO requestPlayerDTO){
        ResponsePlayerDTO responsePlayerDTO = iPlayerService.save(requestPlayerDTO);
        return responsePlayerDTO;
    }

    @GetMapping("{id}")
    public ResponsePlayerDTO getPlayer(@PathVariable("id") int id){
        ResponsePlayerDTO responsePlayerDTO = iPlayerService.findById(id);
        return responsePlayerDTO;
    }

    @DeleteMapping("{id}")
    public void delete(@PathVariable("id") int id){
        iPlayerService.deleteById(id);
    }













}
