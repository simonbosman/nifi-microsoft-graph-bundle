## Connect to AKS and Apache NiFi cluster

To manage a Kubernetes cluster, use the Kubernetes command-line client, `kubectl`. `kubectl` is already installed if you use Azure Cloud Shell. To install `kubectl` locally, use the `az aks install-cli` command.

1. Configure `kubectl` to connect to your Kubernetes cluster using the `az aks get-credentials` command. This command downloads credentials and configures the Kubernetes CLI to use them.  
``` 
az aks get-credentials --resource-group DIS_KUBERNETES --name DIS-AKS
```

2. Verify the connection to your cluster using the `kubectl get` command. This command returns a list of the cluster nodes.  
```
kubectl get nodes
```  
3. The following sample output shows the nodes in the cluster.  
`NAME                                STATUS   ROLES   AGE    VERSION`  
`aks-nodepool1-20707210-vmss000000   Ready    agent   142d   v1.26.6`  
`aks-nodepool1-20707210-vmss000001   Ready    agent   142d   v1.26.6`  
`aks-nodepool1-20707210-vmss000005   Ready    agent   142d   v1.26.6`  
`aks-nodepool1-20707210-vmss000009   Ready    agent   117d   v1.26.6`

4. Connect to Apache NiFi cluster.  
`kubectl port-forward svc/nifi 8080`
