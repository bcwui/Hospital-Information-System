import { createRouter, createWebHistory } from "vue-router";
import HomeView from "../views/HomeView.vue";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: "/",
      name: "home",
      component: HomeView,
    },
    {
      path: "/patient",
      name: "patient",
      component: () => import("../views/PatiEnt.vue"),
      children: [
        {
          path: "",
          name: "medicalAp",
          component: () => import("../views/patient/MedicalAp.vue"),
          children: [
            {
              path: "",
              name: "department",
              component: () => import("@/components/Registrations/DepartmentCard.vue"),
            },
            {
              path: ":department",
              name: "clinic",
              component: () => import("@/components/Registrations/ClinicCard.vue"),
            },
            {
              path: ":department/:clinic",
              name: "clinicDoctor",
              component: () => import("@/components/Registrations/DoctorCard.vue"),
            },
            {
              path: ":department/:clinic/:doctor",
              name: "clinicDoctorSchedule",
              component: () => import("@/components/Registrations/ScheduleCardsView.vue"),
            },
          ],
        },
        {
          path: "chat",
          name: "chat",
          component: () => import("../views/patient/ChatView.vue"),
        },
        {
          path: "appointments",
          name: "myAppointments",
          component: () => import("../views/patient/MyAppointments.vue"),
        },
        {
          path: "myinfo",
          name: "myinfo",
          component: () => import("../views/patient/Myinfo.vue"),
        },
        {
          path: "card",
          name: "card",
          component: () => import("../views/CardView.vue"),
        },
      ],
    },
    {
      path: "/doctor",
      name: "doctor",
      component: () => import("../views/DocTor.vue"),
      children: [
        {
          path: "",
          name: "outpatient",
          component: () => import("../views/doctor/OutPatient.vue"),
          children: [
            {
              path: "",
              name: "allRegistrations",
              component: () => import("@/components/Registrations/allRegistrations.vue"),
            },
            {
              path: ":name",
              name: "detailRegistrations",
              component: () => import("@/components/Registrations/detailRegistrations.vue"),
            },
          ],
        },
        {
          path: "myinfo",
          name: "doctorMyinfo",
          component: () => import("../views/doctor/Myinfo.vue"),
        },
      ],
    },
    {
      path: "/admin",
      name: "admin",
      component: () => import("../views/AdMin.vue"),
      children: [
        {
          path: "",
          name: "crud",
          component: () => import("../views/admin/CRUD.vue"),
          children: [
            {
              path: "",
              name: "crudDepartment",
              component: () => import("@/components/Admin/DepartmentCrudCard.vue"),
            },
            {
              path: ":department",
              name: "crudClinic",
              component: () => import("@/components/Admin/ClinicCrudCard.vue"),
            },
            {
              path: ":department/:clinic",
              name: "crudClinicDoctor",
              component: () => import("@/components/Admin/DoctorCrudCard.vue"),
            },
            {
              path: ":department/:clinic/:doctor",
              name: "crudClinicDoctorSchedule",
              component: () => import("@/components/Admin/ScheduleCrudCardsView.vue"),
            },
          ],
        },
        {
          path: "rag",
          name: "ragManager",
          component: () => import("../views/admin/RagManager.vue"),
        },
      ],
    },
  ],
});

export default router;
