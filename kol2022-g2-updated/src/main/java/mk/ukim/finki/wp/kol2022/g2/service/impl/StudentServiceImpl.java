package mk.ukim.finki.wp.kol2022.g2.service.impl;


import mk.ukim.finki.wp.kol2022.g2.model.Course;
import mk.ukim.finki.wp.kol2022.g2.model.Student;
import mk.ukim.finki.wp.kol2022.g2.model.StudentType;
import mk.ukim.finki.wp.kol2022.g2.model.exceptions.InvalidCourseIdException;
import mk.ukim.finki.wp.kol2022.g2.model.exceptions.InvalidStudentIdException;
import mk.ukim.finki.wp.kol2022.g2.repository.CourseRepository;
import mk.ukim.finki.wp.kol2022.g2.repository.StudentRepository;
import mk.ukim.finki.wp.kol2022.g2.service.StudentService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class StudentServiceImpl implements StudentService, UserDetailsService {

    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final PasswordEncoder passwordEncoder;

    public StudentServiceImpl(StudentRepository studentRepository, CourseRepository courseRepository, PasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<Student> listAll() {
        return studentRepository.findAll();
    }

//    @Override
//    public List<String> listAllEmails() {
//        List<String> lista=new ArrayList<>();
//        for(int i=0;i<studentRepository.findAll().size();i++){
//            lista= Collections.singletonList(studentRepository.findAll().get(i).getEmail());
//        }
//        return lista;
//    }

    @Override
    public Student findById(Long id) {
        return studentRepository.findById(id).orElseThrow(InvalidStudentIdException::new);
    }

    @Override
    public Student create(String name, String email, String password, StudentType type, List<Long> courseId, LocalDate enrollmentDate) {
        List<Course> courses=courseRepository.findAllById(courseId);
        if(courses.isEmpty())
            throw new InvalidCourseIdException();
        Student student=new Student(name, email, passwordEncoder.encode(password), type, courses, enrollmentDate);
        return studentRepository.save(student);
    }

    @Override
    public Student update(Long id, String name, String email, String password, StudentType type, List<Long> coursesId, LocalDate enrollmentDate) {
        Student student=studentRepository.findById(id).orElseThrow(InvalidStudentIdException::new);
        List<Course> courses=courseRepository.findAllById(coursesId);
        if(courses.isEmpty())
            throw new InvalidCourseIdException();
        student.setName(name);
        student.setEmail(email);
        student.setPassword(passwordEncoder.encode(password));
        student.setType(type);
        student.setCourses(courses);
        student.setEnrollmentDate(enrollmentDate);
        return studentRepository.save(student);
    }

    @Override
    public Student delete(Long id) {
        Student student=studentRepository.findById(id).orElseThrow(InvalidStudentIdException::new);
        studentRepository.deleteById(id);
        return student;
    }

    @Override
    public List<Student> filter(Long courseId, Integer yearsOfStudying) {
        if(courseId!=null && yearsOfStudying!=null){
            return studentRepository.findAllByCoursesContainingAndEnrollmentDateBefore(
                    courseRepository.findById(courseId).orElseThrow(InvalidCourseIdException::new),
                    LocalDate.now().minusYears(yearsOfStudying)
            );
        }else if(courseId!=null){
            return studentRepository.findAllByCoursesContaining(courseRepository.findById(courseId).orElseThrow(InvalidCourseIdException::new));
        }else if(yearsOfStudying!=null){
            return studentRepository.findAllByEnrollmentDateBefore(LocalDate.now().minusYears(yearsOfStudying));
        }else {
            return studentRepository.findAll();
        }
    }

    //Потребно е да овозможите пребарување на студенти според курс и времетраење на студиите (време поминато од упис на факулт
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Student student=studentRepository.findByEmail(username).orElseThrow(()-> new UsernameNotFoundException(username));
        return new User(student.getEmail(), student.getPassword(), Stream.of(new SimpleGrantedAuthority(String.format(
                "ROLE_%S", student.getType()
        ))).collect(Collectors.toList()));
    }

}
