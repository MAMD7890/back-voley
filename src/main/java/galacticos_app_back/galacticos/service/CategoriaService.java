package galacticos_app_back.galacticos.service;

import galacticos_app_back.galacticos.entity.Categoria;
import galacticos_app_back.galacticos.repository.CategoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class CategoriaService {
    
    @Autowired
    private CategoriaRepository categoriaRepository;
    
    public List<Categoria> obtenerTodos() {
        return categoriaRepository.findAll();
    }
    
    public Optional<Categoria> obtenerPorId(Integer id) {
        return categoriaRepository.findById(id);
    }
    
    public Categoria crear(Categoria categoria) {
        return categoriaRepository.save(categoria);
    }
    
    public Categoria actualizar(Integer id, Categoria categoria) {
        Optional<Categoria> existente = categoriaRepository.findById(id);
        if (existente.isPresent()) {
            categoria.setIdCategoria(id);
            return categoriaRepository.save(categoria);
        }
        return null;
    }
    
    public void eliminar(Integer id) {
        categoriaRepository.deleteById(id);
    }
}
