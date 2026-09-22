<script setup>
import { onMounted, ref } from 'vue'
import { getDepartments } from '../api/departments'

defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: '请选择所属部门'
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue'])
const departments = ref([])
const loading = ref(false)

async function loadDepartments() {
  loading.value = true
  try {
    departments.value = await getDepartments()
  } catch {
    // The HTTP interceptor already presents the server error.
  } finally {
    loading.value = false
  }
}

onMounted(loadDepartments)
</script>

<template>
  <el-select
    :model-value="modelValue"
    :placeholder="placeholder"
    :disabled="disabled"
    :loading="loading"
    filterable
    clearable
    style="width: 100%"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-option
      v-for="department in departments"
      :key="department"
      :label="department"
      :value="department"
    />
  </el-select>
</template>
