package services;

import entities.FunctionEntity;
import entities.PointEntity;
import entities.PointId;
import repositories.FunctionRepository;
import repositories.PointRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PointService {

    private final PointRepository pointRepository;
    private final FunctionRepository functionRepository;

    public PointService(PointRepository pointRepository, FunctionRepository functionRepository) {
        this.pointRepository = pointRepository;
        this.functionRepository = functionRepository;
    }

    public List<PointEntity> findAll() {
        return pointRepository.findAll();
    }

    public Optional<PointEntity> findById(Long functionId, Double xValue) {
        return pointRepository.findById(new PointId(functionId, xValue));
    }

    public List<PointEntity> findByFunctionId(Long functionId) {
        return pointRepository.findByIdFunctionId(functionId);
    }

    public List<PointEntity> findByFunctionIdAndXValueBetween(Long functionId, Double fromX, Double toX, Sort sort) {
        return pointRepository.findByIdFunctionIdAndIdXValueBetween(functionId, fromX, toX, sort);
    }

    public PointEntity save(PointEntity point) {
        return pointRepository.save(point);
    }

    public PointEntity createPoint(Long functionId, Double xValue, Double yValue) {
        if (functionId == null) {
            throw new IllegalArgumentException("Function ID cannot be null");
        }
        if (xValue == null) {
            throw new IllegalArgumentException("X value cannot be null");
        }
        if (yValue == null) {
            throw new IllegalArgumentException("Y value cannot be null");
        }
        Optional<FunctionEntity> functionOpt = functionRepository.findById(functionId);
        if (functionOpt.isEmpty()) {
            throw new IllegalArgumentException("Function not found with id: " + functionId);
        }
        PointEntity point = new PointEntity(functionOpt.get(), xValue, yValue);
        return pointRepository.save(point);
    }

    public void deleteById(Long functionId, Double xValue) {
        pointRepository.deleteById(new PointId(functionId, xValue));
    }

    public boolean existsById(Long functionId, Double xValue) {
        return pointRepository.existsById(new PointId(functionId, xValue));
    }
}



