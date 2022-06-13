const SectionPage = () => import(/* webpackChunkName: "js/section/section" */'@/views/section/SectionPage')

const sectionRoutes = [
  {
    path: '/sections',
    component: SectionPage
  }
]
export default sectionRoutes
