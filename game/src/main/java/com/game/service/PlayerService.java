package com.game.service;

import com.game.Exceptions.BadRequestException;
import com.game.Exceptions.PlayerNotFoundException;
import com.game.controller.RequestPlayerDTO;
import com.game.controller.ResponsePlayerDTO;
import com.game.entity.Player;
import com.game.repository.PlayerRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.*;

@Service
public class PlayerService implements IPlayerService {

    private final int MAX_EXP = 10_000_000;

    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private PlayerRepository playerRepository;

    @Override
    public Integer getPlayerCounts(RequestPlayerDTO requestPlayerDTO) {
        CriteriaQuery<Player> playerCriteriaQuery  = makeCriteriaQuery(requestPlayerDTO);
        List<Player> playerList = entityManager.createQuery(playerCriteriaQuery).getResultList();
        Integer playersCount = playerList.size();
        return playersCount;
    }

    @Override
    public List<ResponsePlayerDTO> getPlayerList(RequestPlayerDTO requestPlayerDTO) {
        CriteriaQuery<Player> playerCriteriaQuery = makeCriteriaQuery(requestPlayerDTO);
        checkPageSizeAndNumbers(requestPlayerDTO);
        int skip = (requestPlayerDTO.getPageNumber() > 0 ? requestPlayerDTO.getPageNumber() : 0) * requestPlayerDTO.getPageSize();
        List<Player> playerList = entityManager.createQuery(playerCriteriaQuery).setFirstResult(skip).setMaxResults(requestPlayerDTO.getPageSize()).getResultList();
        List<ResponsePlayerDTO> responsePlayerDTOList = new ArrayList<>();
        for (Player player : playerList) {
            responsePlayerDTOList.add(convertPlayerToDTO(player));
        }
        return responsePlayerDTOList;
    }

    @Override
    public ResponsePlayerDTO update(int id, RequestPlayerDTO requestPlayerDTO) {
        isValidId(id);
        existsById(id);

        Optional<Player> optionalPlayer = playerRepository.findById((long) id);
        Player player = optionalPlayer.get();
        player.setId((long) id);
        if (requestPlayerDTO.getName() != null && !(requestPlayerDTO.getName().equals(player.getName()))) {
            player.setName(requestPlayerDTO.getName());
        }

        if (requestPlayerDTO.getTitle() != null && !(requestPlayerDTO.getTitle().equals(player.getTitle()))) {
            player.setTitle(requestPlayerDTO.getTitle());
        }

        if (requestPlayerDTO.getRace() != null && !(requestPlayerDTO.getRace().equals(player.getRace()))) {
            player.setRace(requestPlayerDTO.getRace());
        }

        if (requestPlayerDTO.getProfession() != null && !(requestPlayerDTO.getProfession().equals(player.getProfession()))) {
            player.setProfession(requestPlayerDTO.getProfession());
        }

        if (requestPlayerDTO.getBirthday() != null) {
            if (requestPlayerDTO.getBirthday() < 0) {
                throw new BadRequestException();
            }

            if (requestPlayerDTO.getBirthday() != player.getBirthday().getTime()) {
                Date date = new Date();
                date.setTime(requestPlayerDTO.getBirthday());
                player.setBirthday(date);
            }
        }

        if (requestPlayerDTO.getBanned() != null && !(requestPlayerDTO.getBanned().equals(player.getBanned()))) {
            player.setBanned(requestPlayerDTO.getBanned());
        }

        if (requestPlayerDTO.getExperience() != null && !Objects.equals(requestPlayerDTO.getExperience(), player.getExperience())) {
            if (requestPlayerDTO.getExperience() < 0 || requestPlayerDTO.getExperience() > MAX_EXP) {
                throw new BadRequestException();
            }

            player.setExperience(requestPlayerDTO.getExperience());
            player.setLevel(calculateLvl(player.getExperience()));
            player.setUntilNextLevel(calculateExpUntilNextLvl(player.getLevel(), player.getExperience()));
        }

        playerRepository.save(player);
        ResponsePlayerDTO responsePlayerDTO = convertPlayerToDTO(player);
        return responsePlayerDTO;
    }

    @Override
    public ResponsePlayerDTO save(RequestPlayerDTO requestPlayerDTO) {
        createIsValid(requestPlayerDTO);
        Player player = convertDTOtoPlayer(requestPlayerDTO);
        playerRepository.save(player);
        ResponsePlayerDTO responsePlayerDTO = convertPlayerToDTO(player);

        return responsePlayerDTO;
    }

    @Override
    public ResponsePlayerDTO findById(int id) {
        isValidId(id);
        existsById(id);
        Optional<Player> optionalPlayer = playerRepository.findById((long) id);
        ResponsePlayerDTO playerDTO = convertPlayerToDTO(optionalPlayer.get());

        return playerDTO;
    }

    @Override
    public void deleteById(int id) {
        isValidId(id);
        existsById(id);
        playerRepository.deleteById((long) id);
    }

    private CriteriaQuery<Player> makeCriteriaQuery(RequestPlayerDTO requestPlayerDTO) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Player> playerCriteriaQuery = criteriaBuilder.createQuery(Player.class);
        Root<Player> playerRoot = playerCriteriaQuery.from(Player.class);
        playerCriteriaQuery.select(playerRoot);

        List<Predicate> predicates = new ArrayList<Predicate>();

        if (requestPlayerDTO.getName() != null) {
            Predicate predicate = criteriaBuilder.like(playerRoot.get("name").as(String.class), "%" + requestPlayerDTO.getName() + "%");
            predicates.add(predicate);
        }

        if (requestPlayerDTO.getTitle() != null) {
            Predicate predicate = criteriaBuilder.like(playerRoot.get("title").as(String.class), "%" + requestPlayerDTO.getTitle() + "%");
            predicates.add(predicate);
        }

        if (requestPlayerDTO.getRace() != null) {
            Predicate predicate = criteriaBuilder.equal(playerRoot.get("race"), requestPlayerDTO.getRace());
            predicates.add(predicate);
        }

        if (requestPlayerDTO.getProfession() != null) {
            Predicate predicate = criteriaBuilder.equal(playerRoot.get("profession"), requestPlayerDTO.getProfession());
            predicates.add(predicate);
        }

        if (requestPlayerDTO.getAfter() != null) {
            Date date = new Date();
            date.setTime(requestPlayerDTO.getAfter());
            Predicate predicate = criteriaBuilder.greaterThan(playerRoot.get("birthday"), date);
            predicates.add(predicate);
        }

        if (requestPlayerDTO.getBefore() != null) {
            Date date = new Date();
            date.setTime(requestPlayerDTO.getBefore());
            Predicate predicate = criteriaBuilder.lessThan(playerRoot.get("birthday"), date);
            predicates.add(predicate);
        }

        if (requestPlayerDTO.getBanned() != null) {
            Predicate predicate = criteriaBuilder.equal(playerRoot.get("banned"), requestPlayerDTO.getBanned());
            predicates.add(predicate);
        }

        if (requestPlayerDTO.getMinExperience() != null) {
            Predicate predicate = criteriaBuilder.greaterThan(playerRoot.get("experience"), requestPlayerDTO.getMinExperience());
            predicates.add(predicate);
        }

        if (requestPlayerDTO.getMaxExperience() != null) {
            Predicate predicate = criteriaBuilder.lessThan(playerRoot.get("experience"), requestPlayerDTO.getMaxExperience());
            predicates.add(predicate);
        }

        if (requestPlayerDTO.getMinLevel() != null) {
            Predicate predicate = criteriaBuilder.greaterThan(playerRoot.get("level"), requestPlayerDTO.getMinLevel());
            predicates.add(predicate);
        }

        if (requestPlayerDTO.getMaxLevel() != null) {
            Predicate predicate = criteriaBuilder.lessThan(playerRoot.get("level"), requestPlayerDTO.getMaxLevel());
            predicates.add(predicate);
        }

        if (predicates.size() > 0) {
            Predicate[] predicates1 = predicates.toArray(new Predicate[0]);
            Predicate predicate = criteriaBuilder.and(predicates1);
            playerCriteriaQuery.where(predicate);
        }

        if (requestPlayerDTO.getOrder() != null) {
            playerCriteriaQuery = playerCriteriaQuery.orderBy(criteriaBuilder.asc(playerRoot.get(requestPlayerDTO.getOrder().getFieldName())));
        }

        return playerCriteriaQuery;
    }

    public void existsById(int id) {
        if (!playerRepository.existsById((long) id)) {
            throw new PlayerNotFoundException();
        }
    }

    private void isValidId(int id) {
        if (id <= 0) {
            throw new BadRequestException();
        }
    }

    private void checkPageSizeAndNumbers(RequestPlayerDTO requestPlayerDTO){
        int defaultPageNumber = 0;
        int defaultPageSize = 3;

        if (requestPlayerDTO.getPageNumber() == null) {
            requestPlayerDTO.setPageNumber(defaultPageNumber);
        }

        if (requestPlayerDTO.getPageSize() == null) {
            requestPlayerDTO.setPageSize(defaultPageSize);
        }
    }

    private int calculateLvl(int exp) {
        int lvl = (int) ((Math.sqrt(2500 + 200 * exp) - 50) / 100);
        return lvl;
    }

    private int calculateExpUntilNextLvl(int lvl, int exp) {
        int expUntilNextLvl = 50 * (lvl + 1) * (lvl + 2) - exp;
        return expUntilNextLvl;
    }

    public void createIsValid(RequestPlayerDTO requestPlayerDTO) {
        int maxNameLength = 12;
        int maxTitleLength = 30;
        if (requestPlayerDTO.getName() == null ||
                requestPlayerDTO.getTitle() == null ||
                requestPlayerDTO.getRace() == null ||
                requestPlayerDTO.getProfession() == null ||
                requestPlayerDTO.getName().equals("") && requestPlayerDTO.getName().length() > maxNameLength ||
                requestPlayerDTO.getTitle().length() > maxTitleLength ||
                requestPlayerDTO.getExperience() < 0 || requestPlayerDTO.getExperience() > MAX_EXP) {
            throw new BadRequestException();
        }

        Date date = new Date();
        date.setTime(requestPlayerDTO.getBirthday());
        int minRegistrationDate = 2000 - 1900;
        int maxRegistrationDate = 3000 - 1900;
        if (requestPlayerDTO.getBirthday() < 0 ||
                date.getYear() > maxRegistrationDate ||
                date.getYear() < minRegistrationDate) {
            throw new BadRequestException();
        }

    }

    private Player convertDTOtoPlayer(RequestPlayerDTO playerDTO) {
        Player player = new Player();
        Date date = new Date();
        date.setTime(playerDTO.getBirthday());

        player.setName(playerDTO.getName());
        player.setTitle(playerDTO.getTitle());
        player.setRace(playerDTO.getRace());
        player.setProfession(playerDTO.getProfession());
        player.setExperience(playerDTO.getExperience());
        player.setLevel(calculateLvl(player.getExperience()));
        player.setUntilNextLevel(calculateExpUntilNextLvl(player.getLevel(), player.getExperience()));
        player.setBirthday(date);
        player.setBanned(playerDTO.getBanned());

        return player;
    }

    private ResponsePlayerDTO convertPlayerToDTO(Player player) {
        ResponsePlayerDTO playerDTO = new ResponsePlayerDTO();

        playerDTO.setId(player.getId());
        playerDTO.setName(player.getName());
        playerDTO.setTitle(player.getTitle());
        playerDTO.setRace(player.getRace());
        playerDTO.setProfession(player.getProfession());
        playerDTO.setBanned(player.getBanned());
        playerDTO.setBirthday(player.getBirthday().getTime());
        playerDTO.setExperience(player.getExperience());
        playerDTO.setLevel(player.getLevel());
        playerDTO.setUntilNextLevel(player.getUntilNextLevel());

        return playerDTO;
    }
}
