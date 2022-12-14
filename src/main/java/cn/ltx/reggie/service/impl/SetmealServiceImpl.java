package cn.ltx.reggie.service.impl;

import cn.ltx.reggie.common.CustomException;
import cn.ltx.reggie.common.R;
import cn.ltx.reggie.dto.DishDto;
import cn.ltx.reggie.dto.SetmealDto;
import cn.ltx.reggie.entity.Category;
import cn.ltx.reggie.entity.Setmeal;
import cn.ltx.reggie.entity.SetmealDish;
import cn.ltx.reggie.mapper.SetmealMapper;
import cn.ltx.reggie.service.CategoryService;
import cn.ltx.reggie.service.DishService;
import cn.ltx.reggie.service.SetmealDishService;
import cn.ltx.reggie.service.SetmealService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @PROJECT_NAME: reggie_take_out
 * @PACKAGE_NAME: cn.ltx.reggie.service.impl
 * @CLASS_NAME: SetmealServiceImpl
 * @Author: 21130
 * @CreateTime: 2022-08-24  17:02
 * @Description:
 * @Version: 1.0
 */
@SuppressWarnings("ALL")
@Service
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {
    @Autowired
    private SetmealDishService setmealDishService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private DishService dishService;
    @Override
    public void updateStatus(int status, List<Long> ids) {
        QueryWrapper<Setmeal> wrapper = new QueryWrapper<>();
        wrapper.in("id", ids);
        Setmeal setmeal = new Setmeal();
        setmeal.setStatus(status);
        this.update(setmeal,wrapper);
    }

    @Override
    public void delete(List<Long> ids) {
        QueryWrapper<Setmeal> wrapper = new QueryWrapper<>();
        wrapper.in("id",ids).eq("status", 1);
        int count = this.count(wrapper);
        if(count > 0){
            throw new CustomException("??????????????????????????????????????????????????????");
        }
        QueryWrapper<Setmeal> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", ids);
        this.remove(queryWrapper);
    }

    @Override
    public void saveWithDish(SetmealDto setmealDto) {
        //?????????????????????
        this.save(setmealDto);
        List<SetmealDish> setmealDishList = setmealDto.getSetmealDishes().stream().map((item) -> {
            item.setSetmealId(setmealDto.getId());
            return item;
        }).collect(Collectors.toList());
        //?????????????????????????????????????????????setmeal_dish,??????insert??????
        setmealDishService.saveBatch(setmealDishList);
    }

    @Override
    public SetmealDto getByIdWithDish(Long id) {
        Setmeal setmeal = this.getById(id);
        SetmealDto setmealDto = new SetmealDto();
        List<SetmealDish> setmealDishList = setmealDishService.list(new QueryWrapper<SetmealDish>().eq("setmeal_id", id));
        BeanUtils.copyProperties(setmeal, setmealDto);
        setmealDto.setSetmealDishes(setmealDishList);
        return setmealDto;
    }

    @Override
    public void updateWithDish(SetmealDto setmealDto) {
        this.updateById(setmealDto);
        setmealDishService.removeById(setmealDto.getId());
        List<SetmealDish> setmealDishList = setmealDto.getSetmealDishes().stream().map((item) -> {
            item.setSetmealId(setmealDto.getId());
            return item;
        }).collect(Collectors.toList());
        setmealDishService.saveBatch(setmealDishList);
    }

    @Override
    public R<IPage<SetmealDto>> page(int page, int pageSize, String name) {
        //??????????????????
        IPage<Setmeal> pageInfo = new Page<>(page, pageSize);
        IPage<SetmealDto> setmealDtoIPage = new Page<>();

        //??????????????????
        QueryWrapper<Setmeal> wrapper = new QueryWrapper<>();
        //name??????????????????
        wrapper.like(StringUtils.isNotBlank(StringUtils.trim(name)), "name", name)
                .eq("status",1)
                .orderByDesc("update_time");
        //??????????????????
        pageInfo = this.page(pageInfo, wrapper);
        //????????????
        BeanUtils.copyProperties(pageInfo,setmealDtoIPage,"records");
        //??????DishDto??????????????????????????????????????????????????????
        List<SetmealDto> records = pageInfo.getRecords().stream().map((item) -> {
            SetmealDto setmealDto = new SetmealDto();
            //????????????
            BeanUtils.copyProperties(item, setmealDto, "records");
            Long categoryId = item.getCategoryId();
            //????????????id??????????????????
            Category category = categoryService.getById(categoryId);
            if (category != null) {
                //??????????????????
                setmealDto.setCategoryName(category.getName());
            }
            return setmealDto;
        }).collect(Collectors.toList());
        //??????DishDto????????????
        setmealDtoIPage.setRecords(records);
        return R.success(setmealDtoIPage);
    }

    @Override
    public R<List<SetmealDto>> list(Setmeal setmeal) {
        QueryWrapper<Setmeal> wrapper = new QueryWrapper<>();
        wrapper.eq("category_id", setmeal.getCategoryId()).eq("status", setmeal.getStatus());
        //List<Setmeal> list = this.list(wrapper);
        List<SetmealDto> setmealDtoList = this.list(wrapper).stream().map((item) -> {
            SetmealDto setmealDto = new SetmealDto();
            BeanUtils.copyProperties(item, setmealDto);
            QueryWrapper<SetmealDish> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("setmeal_id",item.getId());
            List<SetmealDish> setmealDishList = setmealDishService.list(queryWrapper);
            setmealDto.setSetmealDishes(setmealDishList);
            return setmealDto;
        }).collect(Collectors.toList());
        return R.success(setmealDtoList);
    }

    /**
     * ????????????id??????????????????????????????????????????????????????
     * @param id
     * @return
     */
    @Override
    public R<List<DishDto>> setmealDishList(Long id) {
        //1?????????????????????????????????????????????
        List<SetmealDish> setmealDishList = setmealDishService.list(new QueryWrapper<SetmealDish>().eq("setmeal_id", id));
        //2???????????????????????????id?????????????????????????????????????????????
        List<DishDto> dishDtoList = setmealDishList.stream().map((item) -> {
            Long dishId = item.getDishId();
            DishDto dishDto = dishService.getByIdWithFlavor(dishId);
            dishDto.setCopies(item.getCopies());
            return dishDto;
        }).collect(Collectors.toList());
        return R.success(dishDtoList);
    }
}
