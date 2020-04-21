# 持续发布相关代码

## 目录结构

- conf/ 可被consul, k8s及服务上线等部署工具所共享的配置文件
- conf/default/ 内为默认配置
- conf/{{environment}}/ 内为不同SaaS环境(如validation, prod)特有的配置，如节点ip地址，资源地址等
- consul/ 内为更新consul的ansible脚本
- 根目录为jenkins的一些pipeline代码

## consul配置说明

约定如下：
1. 默认的服务配置放在conf/default/services.yml下的service_configs主键下
2. 环境特定配置放在conf/{{environment}}/services.yml下的service_additional_configs主键下
3. 最终的配置为上面两个文件合并的结果，service_additional_configs内的定义会覆盖service_configs下的定义

### global下的配置

- resources.yml里的配置和所有服务的url会放入global
- 各服务的地址

### 服务地址的约定

- 服务地址分为内部地址和外部地址
- 外部地址在endpoints.yml的public下面定义
- 服务的内部地址按约定生成，格式为：http://{{servicename}}.service.consul:{{server_port}}，如客户服务的地址为http://customer.service.consul:18009，

   内部地址放在global下的{{servicename}}_internal_url键下，如customer_internal_url

- 没有server_port定义的服务，内部地址放在endpoints.yml的internal下定义，也放在global下


### convertlab/{{servicename}}/下的配置

- 每个服务的配置会被放入convertlab/{{servicename}}/下，如customer的配置将被放在convertlab/customer/下

## 手动运行ansible的方法

```sh
ansible-playbook consul/main.yml -i consul/hosts.ini -e host=test -e folder=test

```